package com.auzienko.javamocks.transaction.persistence.repository;

import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.persistence.BaseIntegrationTest;
import com.auzienko.javamocks.transaction.persistence.PersistenceTestConfiguration;
import com.auzienko.javamocks.transaction.persistence.entity.TransactionEntity;
import com.auzienko.javamocks.transaction.persistence.mapper.TransactionPersistenceMapper;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Testcontainers
class TransactionRepositoryImplIT extends BaseIntegrationTest {

    @Autowired
    private TransactionRepositoryImpl underTest;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Autowired
    private TransactionPersistenceMapper mapper;

    private Transaction newTransaction;

    @BeforeEach
    void setUp() {
        // Prepare a fresh transaction object before each test
        newTransaction = new Transaction(
                "test_user",
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("999.99"),
                Currency.EUR
        );
    }

    @Nested
    @DisplayName("When saving a new transaction")
    class SaveTests {

        @Test
        @DisplayName("it should persist the transaction entity to the database")
        void shouldPersistTransactionEntity() {
            // ACT
            Transaction savedTransaction = underTest.save(newTransaction);

            // ASSERT - Direct database verification
            Optional<TransactionEntity> foundEntityOptional = transactionJpaRepository.findById(savedTransaction.getId());
            assertThat(foundEntityOptional).isPresent();
            TransactionEntity foundEntity = foundEntityOptional.get();
            assertThat(foundEntity.getAmount()).isEqualByComparingTo("999.99");
            assertThat(foundEntity.getCurrency()).isEqualTo(Currency.EUR);
        }

        @Test
        @DisplayName("it should persist the initial PENDING status history")
        @Transactional
        void shouldPersistInitialStatusHistory() {
            // ACT
            Transaction savedTransactionInitial = underTest.save(newTransaction);

            // ASSERT
            // We re-fetch the transaction to ensure we are checking the truly persisted state
            Transaction fetchedTransaction = underTest.findById(savedTransactionInitial.getId()).get();

            assertThat(fetchedTransaction.getId()).isNotNull();
            assertThat(fetchedTransaction.getCreatedAt()).isNotNull();
            assertThat(fetchedTransaction.getCurrentStatus()).isEqualTo(TransactionStatus.PENDING);
        }

        @Test
        @DisplayName("it should return a domain object with generated ID and createdAt")
        void shouldReturnDomainObjectWithGeneratedValues() {
            // ACT
            Transaction savedTransaction = underTest.save(newTransaction);

            // ASSERT - Check the returned object
            assertThat(savedTransaction.getId()).isNotNull();
            assertThat(savedTransaction.getCreatedAt()).isNotNull();
            assertThat(savedTransaction.getCurrentStatus()).isEqualTo(TransactionStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("When finding a transaction by ID")
    class FindByIdTests {

        private Transaction savedTransaction;

        @BeforeEach
        void setupExistingTransaction() {
            // Arrange a pre-existing transaction for find tests
            savedTransaction = underTest.save(newTransaction);
        }

        @Test
        @DisplayName("it should return the correct transaction when ID exists")
        void shouldReturnTransactionWhenIdExists() {
            // ACT
            Optional<Transaction> foundOptional = underTest.findById(savedTransaction.getId());

            // ASSERT
            assertThat(foundOptional).isPresent();
            Transaction found = foundOptional.get();
            assertThat(found.getId()).isEqualTo(savedTransaction.getId());
            assertThat(found.getAmount()).isEqualByComparingTo(savedTransaction.getAmount());
            assertThat(found.getStatusHistory()).hasSize(1);
        }

        @Test
        @DisplayName("it should return an empty Optional when ID does not exist")
        void shouldReturnEmptyWhenIdDoesNotExist() {
            // ACT
            Optional<Transaction> foundOptional = underTest.findById(UUID.randomUUID());

            // ASSERT
            assertThat(foundOptional).isEmpty();
        }
    }
}
