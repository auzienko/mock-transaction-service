package com.auzienko.javamocks.transaction.api.controller;

import com.auzienko.javamocks.transaction.api.mapper.TransactionApiMapper;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import com.auzienko.javamocks.transaction.publicapi.dto.CreateTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Public Transaction API", description = "Endpoints for end-users to manage their transactions")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class TransactionPublicController {

    private final TransactionService transactionService;
    private final TransactionApiMapper apiMapper;

    @Operation(summary = "Get a transaction by its ID",
            description = "Retrieves the details of a specific transaction owned by the current user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the transaction",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found with the given ID",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @Parameter(description = "The UUID of the transaction to retrieve", required = true)
            @PathVariable UUID id,
            Principal principal) {

        String username = getCurrentUsername(principal);

        return transactionService.findTransactionById(id, username)
                .map(apiMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Create a new transaction",
            description = "Initiates a new financial transaction between two accounts.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body provided",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions",
                    content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content)
    })
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Parameter(description = "The transaction request object", required = true,
                    schema = @Schema(implementation = CreateTransactionRequest.class))
            @Valid @RequestBody CreateTransactionRequest request,
            Principal principal) {

        String username = getCurrentUsername(principal);

        Transaction transactionToCreate = apiMapper.toDomain(request, username);
        Transaction createdTransaction = transactionService.createTransaction(transactionToCreate);
        TransactionResponse responseDto = apiMapper.toResponse(createdTransaction);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdTransaction.getId())
                .toUri();

        return ResponseEntity.created(location).body(responseDto);
    }

    String getCurrentUsername(Principal principal) {
        if (principal != null) {
            return principal.getName();
        }

        // Fallback
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        throw new IllegalStateException("No authenticated user found");
    }
}
