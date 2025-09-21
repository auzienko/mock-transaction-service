package com.auzienko.javamocks.transaction.domain.service;

import com.auzienko.javamocks.transaction.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionService {
    /**
     * Initiates a new transaction.
     * The transaction is created with an initial PENDING status.
     *
     * @param transaction The transaction object to be created.
     * @return The persisted transaction with its generated ID and createdAt timestamp.
     */
    Transaction createTransaction(Transaction transaction);

    /**
     * Retrieves a transaction by its unique identifier.
     *
     * @param id     The UUID of the transaction.
     * @return An Optional containing the transaction if found, otherwise empty.
     */
    Optional<Transaction> findTransactionById(UUID id);

    /**
     * Retrieves a transaction by its unique identifier and owner Id.
     *
     * @param id     The UUID of the transaction.
     * @param ownerId      The user id
     * @return An Optional containing the transaction if found, otherwise empty.
     */
    Optional<Transaction> findTransactionById(UUID id, String ownerId);

    /**
     * Marks a transaction as COMPLETED.
     *
     * @param id The UUID of the transaction to complete.
     * @return The updated transaction.
     * @throws com.auzienko.javamocks.transaction.domain.exception.TransactionNotFoundException if transaction not found.
     * @throws IllegalStateException                                                            if the transaction is not in a PENDING state.
     */
    Transaction completeTransaction(UUID id);

    /**
     * Marks a transaction as FAILED.
     *
     * @param id     The UUID of the transaction to fail.
     * @param reason The reason for the failure.
     * @return The updated transaction.
     * @throws com.auzienko.javamocks.transaction.domain.exception.TransactionNotFoundException if transaction not found.
     * @throws IllegalStateException                                                            if the transaction is not in a PENDING state.
     */
    Transaction failTransaction(UUID id, String reason);
}
