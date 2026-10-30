package com.webintegral.composite_gateway.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.webintegral.composite_gateway.exception.GatewayServiceException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

class ProductSummaryServiceTest {

    private HttpServer server;
    private ExecutorService serverExecutor;
    private ExecutorService gatewayExecutor;
    private CountDownLatch allRequestsReceived;
    private ProductSummaryService service;

    @BeforeEach
    void startStubServices() throws IOException {
        allRequestsReceived = new CountDownLatch(3);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        serverExecutor = Executors.newFixedThreadPool(3);
        server.setExecutor(serverExecutor);
        server.createContext("/microservices/products/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/8")) {
                respondWhenConcurrent(exchange, 503, "{\"message\":\"Unavailable\"}");
            } else if (path.endsWith("/999")) {
                respondWhenConcurrent(exchange, 404, "{\"message\":\"Not found\"}");
            } else {
                respondWhenConcurrent(exchange, 200,
                        "{\"id\":7,\"name\":\"Test Laptop\",\"priceUsd\":1200.00}");
            }
        });
        server.createContext("/microservices/inventory/", exchange -> {
            String productId = exchange.getRequestURI().getPath().endsWith("/999") ? "999" : "7";
            respondWhenConcurrent(exchange, 200,
                    "{\"productId\":" + productId
                            + ",\"availableUnits\":15,\"stockStatus\":\"IN_STOCK\"}");
        });
        server.createContext("/v1/latest", exchange ->
                respondWhenConcurrent(exchange, 200,
                        "{\"amount\":1.0,\"base\":\"USD\",\"date\":\"2026-10-29\","
                                + "\"rates\":{\"MXN\":18.7475}}"));
        server.start();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(1000);
        requestFactory.setReadTimeout(3000);
        RestClient restClient = RestClient.builder().requestFactory(requestFactory).build();
        gatewayExecutor = Executors.newFixedThreadPool(3);
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        service = new ProductSummaryService(
                restClient,
                gatewayExecutor,
                baseUrl + "/microservices/products/{id}",
                baseUrl + "/microservices/inventory/{id}",
                baseUrl + "/v1/latest?base=USD&symbols=MXN");
    }

    @AfterEach
    void stopStubServices() {
        server.stop(0);
        serverExecutor.shutdownNow();
        gatewayExecutor.shutdownNow();
    }

    @Test
    void fetchesAllDependenciesConcurrentlyAndConvertsTheRealRate() {
        var result = service.getProductSummary(7L);

        assertEquals(7L, result.productId());
        assertEquals("Test Laptop", result.title());
        assertEquals(new BigDecimal("22497.00"), result.priceMxn());
        assertEquals(new BigDecimal("18.7475"), result.exchangeRate());
        assertEquals(15, result.availableUnits());
        assertEquals("IN_STOCK", result.stockStatus());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void mapsMissingProductToNotFound() {
        GatewayServiceException exception = assertThrows(
                GatewayServiceException.class, () -> service.getProductSummary(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        assertEquals("catalog-service", exception.getService());
    }

    @Test
    void mapsUnavailableUpstreamToServiceUnavailable() {
        GatewayServiceException exception = assertThrows(
                GatewayServiceException.class, () -> service.getProductSummary(8L));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
        assertEquals("catalog-service", exception.getService());
    }

    private void respondWhenConcurrent(HttpExchange exchange, int status, String json) throws IOException {
        allRequestsReceived.countDown();
        try {
            if (!allRequestsReceived.await(2, TimeUnit.SECONDS)) {
                send(exchange, 503, "{\"message\":\"Requests were not concurrent\"}");
                return;
            }
            send(exchange, status, json);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            send(exchange, 500, "{\"message\":\"Interrupted\"}");
        }
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
