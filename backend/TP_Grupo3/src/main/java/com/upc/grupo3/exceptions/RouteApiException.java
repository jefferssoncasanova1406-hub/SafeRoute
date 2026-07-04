package com.upc.grupo3.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class RouteApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected RouteApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
