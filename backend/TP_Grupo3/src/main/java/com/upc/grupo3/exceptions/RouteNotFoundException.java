package com.upc.grupo3.exceptions;

import org.springframework.http.HttpStatus;

public class RouteNotFoundException extends RouteApiException {

    public RouteNotFoundException(String message) {
        this(message, "ROUTE_NOT_FOUND");
    }

    public RouteNotFoundException(String message, String errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode, message);
    }
}
