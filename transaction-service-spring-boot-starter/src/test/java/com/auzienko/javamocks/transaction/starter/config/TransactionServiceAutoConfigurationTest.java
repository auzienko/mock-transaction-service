package com.auzienko.javamocks.transaction.starter.config;

import com.auzienko.javamocks.transaction.starter.client.TransactionServiceClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TransactionServiceAutoConfigurationTest {

    static MockWebServer mockWebServer;

    @Autowired(required = false) // 'required = false' to avoid test failure if bean is not created
    private TransactionServiceClient transactionServiceClient;

    @BeforeAll
    static void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // Dynamically set the URL for our Feign client to point to the MockWebServer
    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("transaction-service.client.url", () -> mockWebServer.url("/").toString());
    }

    // An empty configuration class that triggers Spring Boot's auto-configuration
    @Configuration
    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Test
    void shouldCreateTransactionServiceClientBean() {
        // The main goal of this test is to verify that the starter
        // successfully created the Feign client bean in the application context.
        assertThat(transactionServiceClient).isNotNull();
    }
}
