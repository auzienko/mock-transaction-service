package com.auzienko.javamocks.transaction.domain.model;

import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionStatusHistory {

    private TransactionStatus status;
    private String reason;
    private Instant timestamp;

    public TransactionStatusHistory(TransactionStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
}
