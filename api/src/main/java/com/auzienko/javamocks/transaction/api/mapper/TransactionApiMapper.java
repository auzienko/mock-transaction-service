package com.auzienko.javamocks.transaction.api.mapper;

import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.publicapi.dto.CreateTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionApiMapper {

    @Mapping(target = "status", expression = "java(transaction.getCurrentStatus())")
    TransactionResponse toResponse(Transaction transaction);

    default Transaction toDomain(CreateTransactionRequest request, String ownerId) {
        if (request == null) {
            return null;
        }

        return new Transaction(
                ownerId,
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getCurrency()
        );
    }
}
