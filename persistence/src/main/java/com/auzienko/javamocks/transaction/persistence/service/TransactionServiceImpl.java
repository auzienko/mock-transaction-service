package com.auzienko.javamocks.transaction.persistence.service;

import com.auzienko.javamocks.transaction.domain.exception.ConcurrencyException;
import com.auzienko.javamocks.transaction.domain.exception.TransactionNotFoundException;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.repository.TransactionRepository;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creating new transaction from account {} to {} for amount {} {}, ownerId {}",
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getOwnerId());

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.debug("Successfully persisted transaction with new ID: {}", savedTransaction.getId());

        return savedTransaction;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findTransactionById(UUID id) {
        log.trace("Attempting to find transaction by ID {}", id);
        return transactionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findTransactionById(UUID id, String ownerId) {
        log.trace("Attempting to find transaction by ID {}, ownerId {}", id, ownerId);
        return transactionRepository.findByIdAndOwnerId(id, ownerId);
    }

    @Override
    @Retryable(
            retryFor = {ConcurrencyException.class},
            maxAttemptsExpression = "${service.retry.concurrency-exception.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${service.retry.concurrency-exception.initial-delay-ms}",
                    multiplierExpression = "${service.retry.concurrency-exception.delay-multiplier}",
                    random = true
            )
    )
    public Transaction completeTransaction(UUID id) {
        log.info("Attempting to mark transaction {} as COMPLETED", id);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to find transaction with ID {} to complete.", id);
                    return new TransactionNotFoundException(id);
                });

        transaction.complete();

        try {
            log.info("Attempting to mark transaction {} as COMPLETED", id);
            Transaction updatedTransaction = transactionRepository.save(transaction);
            log.info("Transaction {} successfully marked as COMPLETED", id);
            return updatedTransaction;
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock failed for transaction {}. Attempting retry.", id, e);
            throw new ConcurrencyException("Failed to complete transaction due to concurrent update.", e);
        }
    }

    @Override
    @Retryable(
            retryFor = {ConcurrencyException.class},
            maxAttemptsExpression = "${service.retry.concurrency-exception.max-attempts}",
            backoff = @Backoff(
                    delayExpression = "${service.retry.concurrency-exception.initial-delay-ms}",
                    multiplierExpression = "${service.retry.concurrency-exception.delay-multiplier}",
                    random = true
            )
    )
    public Transaction failTransaction(UUID id, String reason) {
        log.info("Attempting to mark transaction {} as FAILED. Reason: {}", id, reason);

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Failed to find transaction with ID {} to fail.", id);
                    return new TransactionNotFoundException(id);
                });

        transaction.fail(reason);

        try {
            log.info("Attempting to mark transaction {} as FAILED. Reason: {}", id, reason);
            Transaction updatedTransaction = transactionRepository.save(transaction);
            log.info("Transaction {} successfully marked as FAILED", id);
            return updatedTransaction;
        } catch (OptimisticLockingFailureException e) {
            log.warn("Optimistic lock failed for transaction {}. Attempting retry.", id, e);
            throw new ConcurrencyException("Failed to fail transaction due to concurrent update.", e);
        }
    }
}
