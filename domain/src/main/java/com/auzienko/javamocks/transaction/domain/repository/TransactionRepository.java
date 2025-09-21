package com.auzienko.javamocks.transaction.domain.repository;

import com.auzienko.javamocks.transaction.domain.model.Transaction;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository {
    Transaction save(Transaction transaction);
    Optional<Transaction> findById(UUID id);
    Optional<Transaction> findByIdAndOwnerId(UUID id, String ownerId);
}
