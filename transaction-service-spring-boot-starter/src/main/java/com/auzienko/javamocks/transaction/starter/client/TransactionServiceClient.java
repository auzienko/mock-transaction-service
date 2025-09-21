package com.auzienko.javamocks.transaction.starter.client;

import com.auzienko.javamocks.transaction.publicapi.dto.CreateTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.FailTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "transaction-service", url = "${transaction-service.client.url}")
public interface TransactionServiceClient {

    @GetMapping("/api/v1/transactions/{id}")
    TransactionResponse getTransactionById(@PathVariable("id") UUID id);

    @PostMapping("/api/v1/transactions")
    TransactionResponse createTransaction(@RequestBody CreateTransactionRequest request);

    @PostMapping("/api/v1/transactions/{id}/complete")
    TransactionResponse completeTransaction(@PathVariable("id") UUID id);

    @PostMapping("/api/v1/transactions/{id}/fail")
    TransactionResponse failTransaction(@PathVariable("id") UUID id, @RequestBody FailTransactionRequest request);
}
