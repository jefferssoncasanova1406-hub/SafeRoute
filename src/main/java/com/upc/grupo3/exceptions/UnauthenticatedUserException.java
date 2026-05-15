package com.upc.grupo3.exceptions;

public class UnauthenticatedUserException extends RuntimeException {

    public UnauthenticatedUserException(String message) {
        super(message);
    }
}
