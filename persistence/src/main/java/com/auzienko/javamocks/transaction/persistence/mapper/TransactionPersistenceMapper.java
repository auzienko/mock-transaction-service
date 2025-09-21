package com.auzienko.javamocks.transaction.persistence.mapper;

import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.model.TransactionStatusHistory;
import com.auzienko.javamocks.transaction.persistence.entity.TransactionEntity;
import com.auzienko.javamocks.transaction.persistence.entity.TransactionStatusHistoryEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TransactionPersistenceMapper {

    @Mapping(target = "statusHistory", source = "statusHistory")
    TransactionEntity toEntity(Transaction transaction);

    Transaction toDomain(TransactionEntity transactionEntity);

    TransactionStatusHistory toStatusHistoryDomain(TransactionStatusHistoryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    TransactionStatusHistoryEntity toStatusHistoryEntity(TransactionStatusHistory history);

    @AfterMapping
    default void linkStatusHistory(@MappingTarget TransactionEntity transactionEntity) {
        if (transactionEntity.getStatusHistory() != null) {
            transactionEntity.getStatusHistory().forEach(history -> history.setTransaction(transactionEntity));
        }
    }
}
