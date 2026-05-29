package com.upc.grupo3.exceptions;

public class InvalidAdminAccessRequestException extends RuntimeException {

    public InvalidAdminAccessRequestException(String message) {
        super(message);
    }
}
