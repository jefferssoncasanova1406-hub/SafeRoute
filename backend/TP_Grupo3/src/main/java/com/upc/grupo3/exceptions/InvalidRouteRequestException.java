package com.upc.grupo3.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidRouteRequestException extends RouteApiException {

    public InvalidRouteRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_ROUTE_REQUEST", message);
    }
}
