package com.auzienko.javamocks.transaction.starter.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.auzienko.javamocks.transaction.starter.client")
@EnableConfigurationProperties(TransactionServiceClientProperties.class)
public class TransactionServiceAutoConfiguration {
}
