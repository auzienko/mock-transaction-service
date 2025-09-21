package com.auzienko.javamocks.transaction.api.controller;

import com.auzienko.javamocks.transaction.api.mapper.TransactionApiMapper;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import com.auzienko.javamocks.transaction.publicapi.dto.FailTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/transactions")
@Tag(name = "Internal Transaction API", description = "Endpoints for internal system processes")
@SecurityRequirement(name = "internalAuth")
@RequiredArgsConstructor
@Hidden
@Slf4j
public class TransactionInternalController {

    private final TransactionService transactionService;
    private final TransactionApiMapper apiMapper;

    @Operation(summary = "Mark a transaction as COMPLETED",
            description = "Transitions a PENDING transaction to the COMPLETED state. Used by payment processing services.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction completed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict - Transaction is not in a PENDING state",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<TransactionResponse> completeTransaction(
            @Parameter(description = "The UUID of the transaction to complete", required = true)
            @PathVariable UUID id,
            Principal principal) {

        String serviceName = getCurrentServiceName(principal);
        log.info("Service '{}' completing transaction: {}", serviceName, id);

        Transaction completedTransaction = transactionService.completeTransaction(id);
        TransactionResponse response = apiMapper.toResponse(completedTransaction);

        log.info("Transaction {} completed successfully by service '{}'", id, serviceName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Mark a transaction as FAILED",
            description = "Transitions a PENDING transaction to the FAILED state. Used when payment processing fails.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction failed successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict - Transaction is not in a PENDING state",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<TransactionResponse> failTransaction(
            @Parameter(description = "The UUID of the transaction to fail", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Request containing the failure reason", required = true)
            @Valid @RequestBody FailTransactionRequest request,
            Principal principal) {

        String serviceName = getCurrentServiceName(principal);
        log.info("Service '{}' failing transaction: {} with reason: {}", serviceName, id, request.getReason());

        Transaction failedTransaction = transactionService.failTransaction(id, request.getReason());
        TransactionResponse response = apiMapper.toResponse(failedTransaction);

        log.warn("Transaction {} failed by service '{}', reason: {}", id, serviceName, request.getReason());
        return ResponseEntity.ok(response);
    }

    String getCurrentServiceName(Principal principal) {
        if (principal != null) {
            return principal.getName();
        }

        // Fallback
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        throw new IllegalStateException("No authenticated service found");
    }
}
