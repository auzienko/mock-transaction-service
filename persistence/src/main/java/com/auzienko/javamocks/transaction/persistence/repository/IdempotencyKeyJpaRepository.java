package com.auzienko.javamocks.transaction.persistence.repository;

import com.auzienko.javamocks.transaction.persistence.entity.IdempotencyKeyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyEntity, UUID> {
}
