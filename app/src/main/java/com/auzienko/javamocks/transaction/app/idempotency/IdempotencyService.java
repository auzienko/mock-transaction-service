package com.auzienko.javamocks.transaction.app.idempotency;

import com.auzienko.javamocks.transaction.persistence.entity.IdempotencyKeyEntity;
import com.auzienko.javamocks.transaction.persistence.repository.IdempotencyKeyJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyKeyJpaRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyKeyEntity> findKey(UUID key) {
        return repository.findById(key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveKey(IdempotencyKeyEntity key) {
        repository.save(key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteKey(UUID key) {
        repository.deleteById(key);
    }
}