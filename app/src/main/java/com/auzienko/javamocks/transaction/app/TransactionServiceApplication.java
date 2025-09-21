package com.auzienko.javamocks.transaction.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "com.auzienko.javamocks.transaction")
@EnableJpaRepositories(basePackages = "com.auzienko.javamocks.transaction.persistence.repository")
@EntityScan(basePackages = "com.auzienko.javamocks.transaction.persistence.entity")
@EnableRetry
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }

}
