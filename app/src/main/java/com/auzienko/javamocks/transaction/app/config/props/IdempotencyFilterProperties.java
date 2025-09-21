package com.auzienko.javamocks.transaction.app.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "service.filter.idempotency-filter")
@Getter
@Setter
public class IdempotencyFilterProperties {

    /**
     * Enable or disable the request/response logging filter globally.
     */
    private boolean enabled = true;

    /**
     * Maximum payload size in bytes to log for request/response bodies.
     */
    private int maxPayloadSize = 10240; // 10 KB default

    private long raceConditionRetryDelayMs = 1000;

    private int concurrencyMaxRetries = 10;
}
