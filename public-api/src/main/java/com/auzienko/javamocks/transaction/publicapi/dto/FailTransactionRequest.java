package com.auzienko.javamocks.transaction.publicapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FailTransactionRequest {
    @NotBlank(message = "Reason cannot be blank")
    @Size(max = 255, message = "Reason must be less than 255 characters")
    private String reason;
}
