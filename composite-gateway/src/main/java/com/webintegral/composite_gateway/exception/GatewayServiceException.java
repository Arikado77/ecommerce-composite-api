package com.webintegral.composite_gateway.exception;

import org.springframework.http.HttpStatus;

public class GatewayServiceException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String service;

    public GatewayServiceException(HttpStatus httpStatus, String message, String service) {
        super(message);
        this.httpStatus = httpStatus;
        this.service = service;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getService() {
        return service;
    }
}
