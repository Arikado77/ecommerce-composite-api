package com.webintegral.composite_gateway.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import com.webintegral.composite_gateway.dto.ExchangeRateResponse;
import com.webintegral.composite_gateway.dto.InventoryResponse;
import com.webintegral.composite_gateway.dto.ProductResponse;
import com.webintegral.composite_gateway.dto.ProductSummaryResponse;
import com.webintegral.composite_gateway.exception.GatewayServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Service
public class ProductSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(ProductSummaryService.class);
    private static final String CATALOG_SERVICE = "catalog-service";
    private static final String INVENTORY_SERVICE = "inventory-service";
    private static final String EXCHANGE_RATE_SERVICE = "Frankfurter External API";
    private static final String MXN = "MXN";

    private final RestClient restClient;
    private final ExecutorService executor;
    private final String catalogUrl;
    private final String inventoryUrl;
    private final String exchangeRateUrl;

    public ProductSummaryService(
            RestClient restClient,
            ExecutorService compositeExecutor,
            @Value("${services.catalog-url}") String catalogUrl,
            @Value("${services.inventory-url}") String inventoryUrl,
            @Value("${services.exchange-rate-url}") String exchangeRateUrl) {
        this.restClient = restClient;
        this.executor = compositeExecutor;
        this.catalogUrl = catalogUrl;
        this.inventoryUrl = inventoryUrl;
        this.exchangeRateUrl = exchangeRateUrl;
    }

    public ProductSummaryResponse getProductSummary(Long productId) {
        CompletableFuture<ProductResponse> productFuture =
                CompletableFuture.supplyAsync(() -> fetchProduct(productId), executor);
        CompletableFuture<InventoryResponse> inventoryFuture =
                CompletableFuture.supplyAsync(() -> fetchInventory(productId), executor);
        CompletableFuture<ExchangeRateResponse> exchangeRateFuture =
                CompletableFuture.supplyAsync(this::fetchExchangeRate, executor);

        try {
            CompletableFuture.allOf(productFuture, inventoryFuture, exchangeRateFuture).join();
            return createSummary(productId, productFuture.join(), inventoryFuture.join(),
                    exchangeRateFuture.join());
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof GatewayServiceException gatewayException) {
                throw gatewayException;
            }
            logger.error("Unexpected failure while collecting product summary", cause);
            throw new GatewayServiceException(
                    HttpStatus.BAD_GATEWAY, "Una respuesta de un servicio dependiente no es válida.",
                    "composite-gateway");
        }
    }

    private ProductResponse fetchProduct(Long productId) {
        ProductResponse product = get(catalogUrl, ProductResponse.class, CATALOG_SERVICE, productId);
        if (product.id() == null || !productId.equals(product.id())
                || product.name() == null || product.name().isBlank() || product.priceUsd() == null) {
            throw invalidUpstreamResponse(CATALOG_SERVICE);
        }
        return product;
    }

    private InventoryResponse fetchInventory(Long productId) {
        InventoryResponse inventory = get(
                inventoryUrl, InventoryResponse.class, INVENTORY_SERVICE, productId);
        if (inventory.productId() == null || !productId.equals(inventory.productId())
                || inventory.availableUnits() == null || inventory.stockStatus() == null) {
            throw invalidUpstreamResponse(INVENTORY_SERVICE);
        }
        return inventory;
    }

    private ExchangeRateResponse fetchExchangeRate() {
        ExchangeRateResponse exchangeRate =
                get(exchangeRateUrl, ExchangeRateResponse.class, EXCHANGE_RATE_SERVICE);
        if (exchangeRate.rates() == null || exchangeRate.rates().get(MXN) == null
                || exchangeRate.rates().get(MXN).signum() <= 0) {
            throw invalidUpstreamResponse(EXCHANGE_RATE_SERVICE);
        }
        return exchangeRate;
    }

    private <T> T get(String url, Class<T> responseType, String service, Object... uriVariables) {
        try {
            T response = restClient.get()
                    .uri(url, uriVariables)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw invalidUpstreamResponse(service);
            }
            return response;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404
                    && (CATALOG_SERVICE.equals(service) || INVENTORY_SERVICE.equals(service))) {
                String message = CATALOG_SERVICE.equals(service)
                        ? "No existe un producto con ese identificador."
                        : "No existe inventario para el producto solicitado.";
                throw new GatewayServiceException(HttpStatus.NOT_FOUND, message, service);
            }
            HttpStatus status = switch (exception.getStatusCode().value()) {
                case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
                case 504 -> HttpStatus.GATEWAY_TIMEOUT;
                default -> HttpStatus.BAD_GATEWAY;
            };
            throw new GatewayServiceException(
                    status, "El servicio dependiente respondió con un error.", service);
        } catch (ResourceAccessException exception) {
            boolean timedOut = hasTimeoutCause(exception);
            HttpStatus status = timedOut ? HttpStatus.GATEWAY_TIMEOUT : HttpStatus.SERVICE_UNAVAILABLE;
            String message = timedOut
                    ? "Se agotó el tiempo de espera al consultar el servicio dependiente."
                    : "El servicio dependiente no está disponible.";
            throw new GatewayServiceException(status, message, service);
        } catch (RestClientException exception) {
            throw new GatewayServiceException(
                    HttpStatus.BAD_GATEWAY, "La respuesta del servicio dependiente no es válida.", service);
        }
    }

    private ProductSummaryResponse createSummary(
            Long requestedId,
            ProductResponse product,
            InventoryResponse inventory,
            ExchangeRateResponse exchangeRate) {
        BigDecimal rate = exchangeRate.rates().get(MXN);
        BigDecimal priceMxn = product.priceUsd().multiply(rate).setScale(2, RoundingMode.HALF_UP);
        return new ProductSummaryResponse(
                requestedId,
                product.name(),
                product.priceUsd(),
                priceMxn,
                inventory.stockStatus(),
                inventory.availableUnits(),
                EXCHANGE_RATE_SERVICE,
                rate,
                "SUCCESS");
    }

    private GatewayServiceException invalidUpstreamResponse(String service) {
        return new GatewayServiceException(
                HttpStatus.BAD_GATEWAY, "El servicio dependiente devolvió datos incompletos o inválidos.",
                service);
    }

    private boolean hasTimeoutCause(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
