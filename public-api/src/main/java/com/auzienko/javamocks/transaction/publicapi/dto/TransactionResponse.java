package com.auzienko.javamocks.transaction.publicapi.dto;

import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransactionResponse {
    private UUID id;
    private String ownerId;
    private BigDecimal amount;
    private Currency currency;
    private TransactionStatus status;
}
