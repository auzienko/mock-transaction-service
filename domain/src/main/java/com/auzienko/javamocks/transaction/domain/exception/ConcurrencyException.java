package com.auzienko.javamocks.transaction.domain.exception;

public class ConcurrencyException extends RuntimeException {
    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
