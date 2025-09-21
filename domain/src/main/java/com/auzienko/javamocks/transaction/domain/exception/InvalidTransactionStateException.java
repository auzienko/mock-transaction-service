package com.auzienko.javamocks.transaction.domain.exception;

public class InvalidTransactionStateException extends IllegalStateException {
    public InvalidTransactionStateException(String message) {
        super(message);
    }
}
