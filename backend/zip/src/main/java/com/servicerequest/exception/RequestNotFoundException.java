package com.servicerequest.exception;

public class RequestNotFoundException extends RuntimeException {
    public RequestNotFoundException(String requestId) {
        super("Request with id " + requestId + " not found");
    }
}
