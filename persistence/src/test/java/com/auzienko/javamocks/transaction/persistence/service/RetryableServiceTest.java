package com.auzienko.javamocks.transaction.persistence.service;

import com.auzienko.javamocks.transaction.domain.exception.ConcurrencyException;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.repository.TransactionRepository;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.EnableRetry;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
@EnableRetry
class RetryableServiceTest {

    @Configuration
    static class TestConfig {
        @Bean
        TransactionService transactionService(TransactionRepository transactionRepository) {
            return new TransactionServiceImpl(transactionRepository);
        }
    }

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("should succeed on the second attempt after one optimistic lock failure")
    void shouldSucceedOnSecondAttempt() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction("test_user", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100"), Currency.USD);
        transaction.setId(transactionId);

        // ARRANGE
        given(transactionRepository.findById(transactionId)).willReturn(Optional.of(transaction));
        given(transactionRepository.save(any(Transaction.class)))
                .willThrow(new OptimisticLockingFailureException("Fail 1"))
                .willReturn(transaction); // Succeeds on 2nd call

        // ACT
        transactionService.completeTransaction(transactionId);

        // ASSERT
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should fail after all retry attempts (3 from properties) are exhausted")
    void shouldFailAfterAllAttempts() {
        UUID transactionId = UUID.randomUUID();
        Transaction transaction = new Transaction("test_user", UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100"), Currency.USD);
        transaction.setId(transactionId);

        // ARRANGE
        given(transactionRepository.findById(transactionId)).willReturn(Optional.of(transaction));
        given(transactionRepository.save(any(Transaction.class)))
                .willThrow(new OptimisticLockingFailureException("Always fail"));

        // ACT & ASSERT
        assertThatThrownBy(() -> transactionService.completeTransaction(transactionId))
                .isInstanceOf(ConcurrencyException.class);

        verify(transactionRepository, times(3)).save(any(Transaction.class));
    }
}

