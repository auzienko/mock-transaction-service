package com.auzienko.javamocks.transaction.app;

import com.auzienko.javamocks.transaction.persistence.repository.TransactionJpaRepository;
import com.auzienko.javamocks.transaction.publicapi.dto.CreateTransactionRequest;
import com.auzienko.javamocks.transaction.publicapi.dto.TransactionResponse;
import com.auzienko.javamocks.transaction.publicapi.enums.Currency;
import com.auzienko.javamocks.transaction.publicapi.enums.TransactionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TransactionApiIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgresContainer =
            new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TransactionJpaRepository transactionJpaRepository;

    @Value("${service.api.key}")
    private String apiKey;

    @Test
    @DisplayName("POST /api/v1/transactions should create a transaction when authenticated")
    void createTransaction_e2e_shouldSucceedWithAuthentication() {
        // --- ARRANGE ---
        CreateTransactionRequest requestDto = new CreateTransactionRequest();
        requestDto.setSourceAccountId(UUID.randomUUID());
        requestDto.setDestinationAccountId(UUID.randomUUID());
        requestDto.setAmount(new BigDecimal("123.45"));
        requestDto.setCurrency(Currency.USD);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.set("X-User-Roles", "ROLE_USER");
        headers.set("X-User-ID", "test_user");

        HttpEntity<CreateTransactionRequest> requestEntity =
                new HttpEntity<>(requestDto, headers);

        // --- ACT ---
        ResponseEntity<TransactionResponse> response = restTemplate
                .exchange(
                        "/api/v1/transactions",
                        HttpMethod.POST,
                        requestEntity,
                        TransactionResponse.class
                );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(TransactionStatus.PENDING);
    }

    @Test
    @DisplayName("POST /api/v1/transactions should return 403 when not authenticated")
    void createTransaction_e2e_shouldFailWhenNotAuthenticated() {
        // --- ARRANGE ---
        CreateTransactionRequest requestDto = new CreateTransactionRequest();
        requestDto.setSourceAccountId(UUID.randomUUID());
        requestDto.setDestinationAccountId(UUID.randomUUID());
        requestDto.setAmount(new BigDecimal("123.45"));
        requestDto.setCurrency(Currency.USD);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<CreateTransactionRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // --- ACT ---
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/transactions",
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/v1/transactions should return 403 when API key is invalid")
    void createTransaction_e2e_shouldFailWithInvalidApiKey() {
        // --- ARRANGE ---
        CreateTransactionRequest requestDto = new CreateTransactionRequest();
        requestDto.setSourceAccountId(UUID.randomUUID());
        requestDto.setDestinationAccountId(UUID.randomUUID());
        requestDto.setAmount(new BigDecimal("123.45"));
        requestDto.setCurrency(Currency.USD);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", "invalid-key");
        headers.set("Idempotency-Key", UUID.randomUUID().toString());

        HttpEntity<CreateTransactionRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // --- ACT ---
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("test_user", "test_password")
                .exchange(
                        "/api/v1/transactions",
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("POST /api/v1/transactions should return 400 when missing Idempotency-Key")
    void createTransaction_e2e_shouldFailWhenMissingIdempotencyKey() {
        // --- ARRANGE ---
        CreateTransactionRequest requestDto = new CreateTransactionRequest();
        requestDto.setSourceAccountId(UUID.randomUUID());
        requestDto.setDestinationAccountId(UUID.randomUUID());
        requestDto.setAmount(new BigDecimal("123.45"));
        requestDto.setCurrency(Currency.USD);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);

        HttpEntity<CreateTransactionRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // --- ACT ---
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("test_user", "test_password")
                .exchange(
                        "/api/v1/transactions",
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

        // --- ASSERT ---
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("POST /api/v1/transactions with the same Idempotency-Key should not create a duplicate transaction")
    void createTransaction_shouldBeIdempotent() {
        // --- ARRANGE ---
        long initialTransactionCount = transactionJpaRepository.count();

        CreateTransactionRequest requestDto = new CreateTransactionRequest();
        requestDto.setSourceAccountId(UUID.randomUUID());
        requestDto.setDestinationAccountId(UUID.randomUUID());
        requestDto.setAmount(new BigDecimal("123.45"));
        requestDto.setCurrency(Currency.USD);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.set("X-User-Roles", "ROLE_USER");
        headers.set("X-User-ID", "test_user");


        HttpEntity<CreateTransactionRequest> requestEntity =
                new HttpEntity<>(requestDto, headers);

        // --- ACT: First Request ---
        ResponseEntity<TransactionResponse> firstResponse = restTemplate.exchange(
                "/api/v1/transactions",
                HttpMethod.POST,
                requestEntity,
                TransactionResponse.class
        );

        // --- ASSERT: First Request ---
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID createdTransactionId = firstResponse.getBody().getId();
        assertThat(createdTransactionId).isNotNull();
        assertThat(transactionJpaRepository.count()).isEqualTo(initialTransactionCount + 1);

        // --- ACT: Second (Duplicate) Request ---
        ResponseEntity<TransactionResponse> secondResponse = restTemplate.exchange(
                "/api/v1/transactions",
                HttpMethod.POST,
                requestEntity,
                TransactionResponse.class
        );

        // --- ASSERT: Second Request ---
        assertThat(secondResponse.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(secondResponse.getBody().getId()).isEqualTo(createdTransactionId);
        assertThat(transactionJpaRepository.count()).isEqualTo(initialTransactionCount + 1);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} should return transaction for authenticated user")
    void getTransaction_e2e_shouldSucceedWithAuthentication() {
        // --- ARRANGE ---
        CreateTransactionRequest createRequest = new CreateTransactionRequest();
        createRequest.setSourceAccountId(UUID.randomUUID());
        createRequest.setDestinationAccountId(UUID.randomUUID());
        createRequest.setAmount(new BigDecimal("100.00"));
        createRequest.setCurrency(Currency.EUR);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        headers.set("X-User-Roles", "ROLE_USER");
        headers.set("X-User-ID", "test_user");

        HttpEntity<CreateTransactionRequest> createEntity =
                new HttpEntity<>(createRequest, headers);

        ResponseEntity<TransactionResponse> createResponse = restTemplate.exchange(
                "/api/v1/transactions",
                HttpMethod.POST,
                createEntity,
                TransactionResponse.class
        );

        UUID transactionId = createResponse.getBody().getId();

        ResponseEntity<TransactionResponse> getResponse = restTemplate.exchange(
                "/api/v1/transactions/" + transactionId,
                HttpMethod.GET,
                new HttpEntity<Object>(headers),
                TransactionResponse.class
        );

        // --- ASSERT ---
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotNull();
        assertThat(getResponse.getBody().getId()).isEqualTo(transactionId);
        assertThat(getResponse.getBody().getAmount().compareTo(new BigDecimal("100.00"))).isEqualTo(0);
        assertThat(getResponse.getBody().getCurrency()).isEqualTo(Currency.EUR);
    }
}
