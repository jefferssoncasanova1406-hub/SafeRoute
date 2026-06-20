package com.upc.grupo3.exceptions;

import org.springframework.http.HttpStatus;

public class MapboxGeocodingException extends RouteApiException {

    public MapboxGeocodingException(String message) {
        super(HttpStatus.BAD_GATEWAY, "MAPBOX_SERVICE_ERROR", message);
    }
}
