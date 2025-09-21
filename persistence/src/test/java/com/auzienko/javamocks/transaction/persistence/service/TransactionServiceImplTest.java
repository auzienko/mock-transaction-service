package com.auzienko.javamocks.transaction.persistence.service;

import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.repository.TransactionRepository;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl underTest;

    @Test
    @DisplayName("Should save and return transaction when creating a new valid transaction")
    void createTransaction_shouldSaveAndReturnTransaction() {
        // --- ARRANGE ---
        Transaction inputTransaction = new Transaction(
                "test_user",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                Currency.USD
        );
        assertEquals(TransactionStatus.PENDING, inputTransaction.getCurrentStatus());

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            // The initial status is already part of the object, no need to set it here.
            return saved;
        });

        // --- ACT ---
        Transaction result = underTest.createTransaction(inputTransaction);

        // --- ASSERT ---
        assertNotNull(result, "The returned transaction should not be null.");
        assertNotNull(result.getId(), "The saved transaction should have an ID.");
        assertNotNull(result.getCreatedAt(), "The saved transaction should have a creation timestamp.");
        assertEquals(TransactionStatus.PENDING, result.getCurrentStatus(), "The current status should be PENDING.");
        assertEquals(1, result.getStatusHistory().size(), "Status history should contain one entry.");

        verify(transactionRepository).save(inputTransaction);
    }

    @Test
    @DisplayName("Should return transaction in Optional when transaction exists")
    void getTransactionById_shouldReturnTransaction_whenExists() {
        // --- ARRANGE ---
        UUID existingId = UUID.randomUUID();
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(existingId);
        existingTransaction.setAmount(new BigDecimal("250.00"));
        String user = "test_user";

        when(transactionRepository.findByIdAndOwnerId(existingId, user)).thenReturn(Optional.of(existingTransaction));

        // --- ACT ---
        Optional<Transaction> result = underTest.findTransactionById(existingId, user);

        // --- ASSERT ---
        assertTrue(result.isPresent(), "The result should not be empty.");
        assertEquals(existingId, result.get().getId(), "The ID of the found transaction should match.");
        assertEquals(new BigDecimal("250.00"), result.get().getAmount(), "The amount should match.");

        // Verify that the repository's findById method was called exactly once with the correct ID
        verify(transactionRepository).findByIdAndOwnerId(existingId, user);
    }

    @Test
    @DisplayName("Should return empty Optional when transaction does not exist")
    void getTransactionById_shouldReturnEmptyOptional_whenNotExists() {
        // --- ARRANGE ---
        UUID nonExistentId = UUID.randomUUID();
        String user = "test_user";

        when(transactionRepository.findByIdAndOwnerId(nonExistentId, user)).thenReturn(Optional.empty());

        // --- ACT ---
        Optional<Transaction> result = underTest.findTransactionById(nonExistentId, user);

        // --- ASSERT ---
        assertFalse(result.isPresent(), "The result should be an empty Optional.");

        // Verify the repository method was still called
        verify(transactionRepository).findByIdAndOwnerId(nonExistentId, user);
    }
}
