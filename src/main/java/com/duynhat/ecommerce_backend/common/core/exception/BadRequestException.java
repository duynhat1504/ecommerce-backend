package com.duynhat.ecommerce_backend.common.core.exception;

public class BadRequestException extends RuntimeException{

    public BadRequestException(String message) {
        super(message);
    }
}
