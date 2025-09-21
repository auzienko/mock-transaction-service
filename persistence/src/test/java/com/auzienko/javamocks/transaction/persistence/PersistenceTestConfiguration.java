package com.auzienko.javamocks.transaction.persistence;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = "com.auzienko.javamocks.transaction.persistence")
@EnableJpaRepositories(basePackages = "com.auzienko.javamocks.transaction.persistence.repository")
@EntityScan(basePackages = "com.auzienko.javamocks.transaction.persistence.entity")
public class PersistenceTestConfiguration {
}
