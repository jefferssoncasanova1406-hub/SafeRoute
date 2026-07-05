package com.upc.grupo3.exceptions;

public class DuplicateCommunityVoteException extends RuntimeException {

    public DuplicateCommunityVoteException(String message) {
        super(message);
    }
}
