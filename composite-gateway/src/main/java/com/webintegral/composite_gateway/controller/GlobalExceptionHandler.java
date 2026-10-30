package com.webintegral.composite_gateway.controller;

import com.webintegral.composite_gateway.dto.ErrorResponse;
import com.webintegral.composite_gateway.exception.GatewayServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GatewayServiceException.class)
    public ResponseEntity<ErrorResponse> handleGatewayException(GatewayServiceException exception) {
        ErrorResponse body = new ErrorResponse("ERROR", exception.getMessage(), exception.getService());
        return ResponseEntity.status(exception.getHttpStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProductId(
            MethodArgumentTypeMismatchException exception) {
        ErrorResponse body = new ErrorResponse(
                "ERROR", "El identificador del producto debe ser numérico.", "composite-gateway");
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception exception) {
        logger.error("Unexpected error while creating the product summary", exception);
        ErrorResponse body = new ErrorResponse(
                "ERROR", "Ocurrió un error interno al consultar el producto.", "composite-gateway");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
