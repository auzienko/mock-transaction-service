package com.auzienko.javamocks.transaction.api.controller;

import com.auzienko.javamocks.transaction.api.exception.GlobalExceptionHandler;
import com.auzienko.javamocks.transaction.api.mapper.TransactionApiMapper;
import com.auzienko.javamocks.transaction.domain.model.Transaction;
import com.auzienko.javamocks.transaction.domain.service.TransactionService;
import com.auzienko.javamocks.transaction.publicapi.dto.CreateTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionPublicControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private TransactionApiMapper transactionApiMapper;

    @InjectMocks
    private TransactionPublicController transactionPublicController;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionPublicController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Principal mockPrincipal(String username) {
        return () -> username;
    }

    @Nested
    @DisplayName("GET /api/v1/transactions/{id}")
    class GetTransaction {

        @Test
        @DisplayName("should return transaction when ID exists")
        void getTransactionById_shouldReturnTransaction_whenFound() throws Exception {
            // --- ARRANGE ---
            UUID transactionId = UUID.randomUUID();
            String username = "test_user";

            Transaction domainTransaction = new Transaction();
            domainTransaction.setId(transactionId);

            TransactionResponse responseDto = new TransactionResponse();
            responseDto.setId(transactionId);
            responseDto.setAmount(new BigDecimal("100.00"));
            responseDto.setCurrency(Currency.USD);

            given(transactionService.findTransactionById(eq(transactionId), eq(username)))
                    .willReturn(Optional.of(domainTransaction));
            given(transactionApiMapper.toResponse(domainTransaction)).willReturn(responseDto);

            // --- ACT & ASSERT ---
            mockMvc.perform(get("/api/v1/transactions/{id}", transactionId)
                            .principal(mockPrincipal(username)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(transactionId.toString()))
                    .andExpect(jsonPath("$.amount").value(100.00));
        }

        @Test
        @DisplayName("should return 404 Not Found when ID does not exist")
        void getTransactionById_shouldReturnNotFound_whenNotFound() throws Exception {
            // --- ARRANGE ---
            UUID nonExistentId = UUID.randomUUID();
            String username = "test_user";

            given(transactionService.findTransactionById(eq(nonExistentId), eq(username)))
                    .willReturn(Optional.empty());

            // --- ACT & ASSERT ---
            mockMvc.perform(get("/api/v1/transactions/{id}", nonExistentId)
                            .principal(mockPrincipal(username)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should work with different user")
        void getTransactionById_shouldWorkWithDifferentUser() throws Exception {
            UUID transactionId = UUID.randomUUID();
            String username = "another_user";

            Transaction domainTransaction = new Transaction();
            domainTransaction.setId(transactionId);

            TransactionResponse responseDto = new TransactionResponse();
            responseDto.setId(transactionId);

            given(transactionService.findTransactionById(eq(transactionId), eq(username)))
                    .willReturn(Optional.of(domainTransaction));
            given(transactionApiMapper.toResponse(domainTransaction)).willReturn(responseDto);

            mockMvc.perform(get("/api/v1/transactions/{id}", transactionId)
                            .principal(mockPrincipal(username)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("should create transaction and return 201 Created")
        void createTransaction_shouldReturn201Created() throws Exception {
            // --- ARRANGE ---
            String username = "test_user";

            CreateTransactionRequest requestDto = new CreateTransactionRequest();
            requestDto.setSourceAccountId(UUID.randomUUID());
            requestDto.setDestinationAccountId(UUID.randomUUID());
            requestDto.setAmount(new BigDecimal("500.00"));
            requestDto.setCurrency(Currency.GBP);

            Transaction domainToCreate = new Transaction(
                    username,
                    requestDto.getSourceAccountId(),
                    requestDto.getDestinationAccountId(),
                    requestDto.getAmount(),
                    requestDto.getCurrency()
            );

            Transaction savedDomain = new Transaction();
            UUID newId = UUID.randomUUID();
            savedDomain.setId(newId);

            TransactionResponse responseDto = new TransactionResponse();
            responseDto.setId(newId);

            given(transactionApiMapper.toDomain(any(CreateTransactionRequest.class), eq(username)))
                    .willReturn(domainToCreate);
            given(transactionService.createTransaction(any(Transaction.class))).willReturn(savedDomain);
            given(transactionApiMapper.toResponse(savedDomain)).willReturn(responseDto);

            // --- ACT & ASSERT ---
            mockMvc.perform(post("/api/v1/transactions")
                            .principal(mockPrincipal(username))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Location", "http://localhost/api/v1/transactions/" + newId))
                    .andExpect(jsonPath("$.id").value(newId.toString()));
        }

        @Test
        @DisplayName("should return 400 Bad Request when request body is invalid")
        void createTransaction_shouldReturn400WhenRequestIsInvalid() throws Exception {
            // ARRANGE
            CreateTransactionRequest invalidRequest = new CreateTransactionRequest();
            invalidRequest.setAmount(new BigDecimal("-100")); // Invalid negative amount
            invalidRequest.setCurrency(null); // Invalid null currency

            // ACT & ASSERT
            mockMvc.perform(post("/api/v1/transactions")
                            .principal(mockPrincipal("test_user"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("getCurrentUsername() method")
    class GetCurrentUsername {

        @Test
        @DisplayName("should return username from Principal")
        void getCurrentUsername_shouldReturnUsernameFromPrincipal() {
            // ARRANGE
            String expectedUsername = "test_user";
            Principal principal = mockPrincipal(expectedUsername);

            // ACT
            String actualUsername = transactionPublicController.getCurrentUsername(principal);

            // ASSERT
            assertEquals(expectedUsername, actualUsername);
        }

        @Test
        @DisplayName("should throw IllegalStateException when no Principal")
        void getCurrentUsername_shouldThrowWhenNoPrincipal() {
            // ACT & ASSERT
            assertThrows(IllegalStateException.class, () ->
                    transactionPublicController.getCurrentUsername(null));
        }
    }
}