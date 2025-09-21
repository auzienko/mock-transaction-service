package com.auzienko.javamocks.transaction.api.controller;

import com.auzienko.javamocks.transaction.api.exception.GlobalExceptionHandler;
import com.auzienko.javamocks.transaction.api.mapper.TransactionApiMapper;
import com.auzienko.javamocks.transaction.domain.exception.InvalidTransactionStateException;
import com.auzienko.javamocks.transaction.domain.exception.TransactionNotFoundException;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import com.auzienko.javamocks.transaction.publicapi.dto.FailTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionInternalControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionApiMapper transactionApiMapper;

    @InjectMocks
    private TransactionInternalController transactionInternalController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionInternalController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Principal mockPrincipal(String serviceName) {
        return () -> serviceName;
    }

    @Nested
    @DisplayName("POST /internal/api/v1/transactions/{id}/complete")
    class CompleteTransaction {

        @Test
        @DisplayName("should return 200 OK when transaction is completed successfully")
        void completeTransaction_shouldReturn200Ok() throws Exception {
            // ARRANGE
            UUID transactionId = UUID.randomUUID();
            Transaction completedTransaction = new Transaction();
            completedTransaction.setId(transactionId);
            String serviceName = "test_serviceName";

            given(transactionService.completeTransaction(transactionId)).willReturn(completedTransaction);
            given(transactionApiMapper.toResponse(completedTransaction)).willReturn(new TransactionResponse());

            // ACT & ASSERT
            mockMvc.perform(post("/internal/api/v1/transactions/{id}/complete", transactionId)
                            .principal(mockPrincipal(serviceName)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 404 Not Found when transaction does not exist")
        void completeTransaction_shouldReturn404NotFound() throws Exception {
            // ARRANGE
            UUID transactionId = UUID.randomUUID();
            String serviceName = "test_serviceName";

            given(transactionService.completeTransaction(transactionId)).willThrow(new TransactionNotFoundException(transactionId));

            // ACT & ASSERT
            mockMvc.perform(post("/internal/api/v1/transactions/{id}/complete", transactionId)
                            .principal(mockPrincipal(serviceName)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.statusCode").value(404))
                    .andExpect(jsonPath("$.message").value("Transaction not found with ID: " + transactionId));
        }

        @Test
        @DisplayName("should return 409 Conflict when transaction is not PENDING")
        void completeTransaction_shouldReturn409Conflict() throws Exception {
            // ARRANGE
            UUID transactionId = UUID.randomUUID();
            String serviceName = "test_serviceName";

            given(transactionService.completeTransaction(transactionId)).willThrow(new InvalidTransactionStateException("Only a PENDING transaction..."));

            // ACT & ASSERT
            mockMvc.perform(post("/internal/api/v1/transactions/{id}/complete", transactionId)
                            .principal(mockPrincipal(serviceName)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.statusCode").value(409));
        }

    }

    @Nested
    @DisplayName("POST /internal/api/v1/transactions/{id}/fail")
    class FailTransaction {

        @Test
        @DisplayName("should return 200 OK and the updated transaction")
        void failTransaction_shouldReturn200OkOnSuccess() throws Exception {
            // ARRANGE
            UUID transactionId = UUID.randomUUID();
            FailTransactionRequest request = new FailTransactionRequest();
            request.setReason("Insufficient funds");
            String serviceName = "test_serviceName";

            Transaction failedTransaction = new Transaction(/*...*/);
            TransactionResponse responseDto = new TransactionResponse();

            given(transactionService.failTransaction(transactionId, "Insufficient funds")).willReturn(failedTransaction);
            given(transactionApiMapper.toResponse(failedTransaction)).willReturn(responseDto);

            // ACT & ASSERT
            mockMvc.perform(post("/internal/api/v1/transactions/{id}/fail", transactionId)
                            .principal(mockPrincipal(serviceName))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 400 Bad Request when reason is blank")
        void failTransaction_shouldReturn400WhenReasonIsBlank() throws Exception {
            // ARRANGE
            UUID transactionId = UUID.randomUUID();
            FailTransactionRequest request = new FailTransactionRequest();
            request.setReason(" "); // Blank reason
            String serviceName = "test_serviceName";

            // ACT & ASSERT
            mockMvc.perform(post("/internal/api/v1/transactions/{id}/fail", transactionId)
                            .principal(mockPrincipal(serviceName))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

    }
}
