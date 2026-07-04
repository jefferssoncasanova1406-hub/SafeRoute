package com.upc.grupo3.exceptions;

import org.springframework.http.HttpStatus;

public class MapboxDirectionsException extends RouteApiException {

    public MapboxDirectionsException(String message) {
        super(HttpStatus.BAD_GATEWAY, "MAPBOX_SERVICE_ERROR", message);
    }
}
