package com.upc.grupo3.exceptions;

public class InvalidAuthorizationHeaderException extends RuntimeException {

    public InvalidAuthorizationHeaderException(String message) {
        super(message);
    }
}
