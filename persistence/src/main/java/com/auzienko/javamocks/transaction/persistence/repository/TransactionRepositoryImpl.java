package com.auzienko.javamocks.transaction.persistence.repository;

import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.repository.TransactionRepository;
import com.auzienko.javamocks.transaction.persistence.entity.TransactionEntity;
import com.auzienko.javamocks.transaction.persistence.mapper.TransactionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {

    private final TransactionJpaRepository jpaRepository;
    private final TransactionPersistenceMapper mapper;

    @Override
    @Transactional
    public Transaction save(Transaction transaction) {
        TransactionEntity entityToSave = mapper.toEntity(transaction);
        TransactionEntity savedEntity = jpaRepository.save(entityToSave);
        jpaRepository.flush();
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Transaction> findByIdAndOwnerId(UUID id, String ownerId) {
        return jpaRepository.findByIdAndOwnerId(id, ownerId)
                .map(mapper::toDomain);
    }
}
