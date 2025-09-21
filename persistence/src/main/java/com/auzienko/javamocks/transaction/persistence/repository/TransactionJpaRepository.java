package com.auzienko.javamocks.transaction.persistence.repository;

import com.auzienko.javamocks.transaction.persistence.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {
    Optional<TransactionEntity> findByIdAndOwnerId(UUID id, String ownerId);
}
