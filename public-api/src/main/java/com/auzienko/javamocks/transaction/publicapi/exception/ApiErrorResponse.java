package com.auzienko.javamocks.transaction.publicapi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class ApiErrorResponse {

    private int statusCode;
    private String message;
    private Instant timestamp;
}
