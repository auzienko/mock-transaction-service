package com.auzienko.javamocks.transaction.publicapi.dto;

import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CreateTransactionRequest {

    @NotNull(message = "Source account ID cannot be null.")
    private UUID sourceAccountId;

    @NotNull(message = "Destination account ID cannot be null.")
    private UUID destinationAccountId;

    @NotNull(message = "Amount cannot be null.")
    @Positive(message = "Transaction amount must be positive.")
    private BigDecimal amount;

    @NotNull(message = "Currency code cannot be null.")
    private Currency currency;

}
