package com.auzienko.javamocks.transaction.domain.model;

import com.auzienko.javamocks.transaction.domain.exception.InvalidTransactionStateException;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Data
@NoArgsConstructor
public class Transaction {

    private UUID id;

    @NotNull(message = "Owner ID cannot be null.")
    private String ownerId;

    @NotNull(message = "Source account ID cannot be null.")
    private UUID sourceAccountId;

    @NotNull(message = "Destination account ID cannot be null.")
    private UUID destinationAccountId;

    @NotNull(message = "Amount cannot be null.")
    @Positive(message = "Transaction amount must be positive.")
    private BigDecimal amount;

    @NotNull(message = "Currency cannot be null.")
    private Currency currency;

    private Instant createdAt;

    private List<TransactionStatusHistory> statusHistory = new ArrayList<>();

    public Transaction(String ownerId, UUID sourceAccountId, UUID destinationAccountId, BigDecimal amount, Currency currency) {
        this.ownerId = ownerId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        // The initial status is added to the history. 'createdAt' will be set upon persistence.
        this.statusHistory.add(new TransactionStatusHistory(TransactionStatus.PENDING, "Transaction initiated."));
        validate();
    }

    // --- Business Logic Methods ---

    /**
     * Gets the current status of the transaction by finding the latest entry in its history.
     *
     * @return The current TransactionStatus.
     * @throws IllegalStateException if the transaction has no status history, which represents an invalid state.
     */
    public TransactionStatus getCurrentStatus() {
        return findLatestStatusHistory()
                .map(TransactionStatusHistory::getStatus)
                .orElseThrow(() -> new IllegalStateException("Transaction " + id + " has no status history, which is an invalid state."));
    }

    /**
     * Finds the most recent status history entry.
     *
     * @return an Optional containing the latest TransactionStatusHistory entry, or empty if none exist.
     */
    public Optional<TransactionStatusHistory> findLatestStatusHistory() {
        return statusHistory.stream()
                .max(Comparator.comparing(TransactionStatusHistory::getTimestamp,
                        Comparator.nullsFirst(Comparator.naturalOrder())));
    }

    /**
     * Transitions the transaction to the COMPLETED state.
     * This is a state-mutating business method that enforces transition rules.
     *
     * @throws InvalidTransactionStateException if the transaction is not in a PENDING state.
     */
    public void complete() {
        if (getCurrentStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException("Only a PENDING transaction can be completed. Current status: " + getCurrentStatus());
        }
        this.statusHistory.add(new TransactionStatusHistory(TransactionStatus.COMPLETED, "Transaction processed successfully."));
    }

    /**
     * Transitions the transaction to the FAILED state.
     * This is a state-mutating business method that enforces transition rules.
     *
     * @param reason A description of why the transaction failed.
     * @throws InvalidTransactionStateException if the transaction is not in a PENDING state.
     */
    public void fail(String reason) {
        if (getCurrentStatus() != TransactionStatus.PENDING) {
            throw new InvalidTransactionStateException("Only a PENDING transaction can be failed. Current status: " + getCurrentStatus());
        }
        this.statusHistory.add(new TransactionStatusHistory(TransactionStatus.FAILED, reason));
    }


    // --- Validation Method ---

    /**
     * Validates the internal state and business invariants of the transaction object.
     * This method is called upon creation to ensure the object is always in a valid state.
     *
     * @throws IllegalArgumentException if an invariant is violated.
     */
    private void validate() {
        if (sourceAccountId != null && sourceAccountId.equals(destinationAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same.");
        }
    }
}