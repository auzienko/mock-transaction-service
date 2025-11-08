package com.auzienko.javamocks.transaction.app.filter;

import com.auzienko.javamocks.transaction.app.config.props.IdempotencyFilterProperties;
import com.auzienko.javamocks.transaction.persistence.entity.IdempotencyKeyEntity;
import com.auzienko.javamocks.transaction.persistence.repository.IdempotencyKeyJpaRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final AntPathMatcher pathMatcher = new AntPathMatcher();
    private static final int PROCESSING_STATUS = -1;

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/actuator/**",
            "/health/**",
            "/metrics/**",
            "/error"
    );

    private static final List<String> ALLOWED_METHODS = List.of(
            HttpMethod.GET.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name()
    );

    private final IdempotencyKeyJpaRepository idempotencyKeyRepository;
    private final TransactionTemplate transactionTemplate;
    private final IdempotencyFilterProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return true;
        }

        String method = request.getMethod();
        if (ALLOWED_METHODS.contains(method)) {
            return true;
        }

        String requestPath = request.getRequestURI();
        boolean isExcluded = EXCLUDED_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, requestPath));

        if (isExcluded) {
            log.debug("Idempotency check skipped for excluded path: {}", requestPath);
            return true;
        }

        log.debug("Idempotency check required for path: {} (method: {})", requestPath, method);
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // payload guard
        if (request.getContentLengthLong() > properties.getMaxPayloadSize()) {
            log.warn("Request payload too large: {} bytes for {}",
                    request.getContentLengthLong(), request.getRequestURI());
            sendError(response, HttpStatus.REQUEST_ENTITY_TOO_LARGE,
                    "Request payload exceeds maximum allowed size");
            return;
        }

        // Idempotency-Key check
        String idempotencyKeyStr = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKeyStr == null || idempotencyKeyStr.trim().isEmpty()) {
            log.error("SECURITY VIOLATION: Missing required Idempotency-Key header for {} {}",
                    request.getMethod(), request.getRequestURI());
            sendError(response, HttpStatus.BAD_REQUEST,
                    String.format("Idempotency-Key header is required for %s operations. " +
                                    "This prevents accidental duplicate processing.",
                            request.getMethod()));
            return;
        }

        UUID idempotencyKey;
        try {
            idempotencyKey = UUID.fromString(idempotencyKeyStr.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid Idempotency-Key format: {} for {} {}",
                    idempotencyKeyStr, request.getMethod(), request.getRequestURI());
            sendError(response, HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must be a valid UUID format");
            return;
        }

        handleIdempotentRequest(request, response, filterChain, idempotencyKey);
    }

    private void handleIdempotentRequest(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain,
                                         UUID idempotencyKey) throws IOException, ServletException {

        String operation = request.getMethod() + " " + request.getRequestURI();

        Optional<IdempotencyKeyEntity> existingResponse = findExistingResponse(idempotencyKey);
        if (existingResponse.isPresent()) {
            IdempotencyKeyEntity cached = existingResponse.get();

            if (isCompletedResponse(cached)) {
                log.info("Returning cached response for idempotency key {} ({})", idempotencyKey, operation);
                writeCachedResponse(response, cached);
                return;
            }

            if (isProcessingResponse(cached)) {
                log.info("Request with key {} is being processed concurrently ({})", idempotencyKey, operation);
                waitForCompletion(response, idempotencyKey, operation);
                return;
            }
        }

        boolean lockAcquired = tryAcquireLock(idempotencyKey, operation);
        if (!lockAcquired) {
            log.info("Lock acquisition failed for key {} ({}), waiting for completion", idempotencyKey, operation);
            waitForCompletion(response, idempotencyKey, operation);
            return;
        }

        try {
            processAndCacheResponse(request, response, filterChain, idempotencyKey, operation);
        } catch (Exception e) {
            log.error("Error processing idempotent request {} with key {}", operation, idempotencyKey, e);
            releaseLock(idempotencyKey);
            throw e;
        }
    }

    private void waitForCompletion(HttpServletResponse response, UUID idempotencyKey, String operation) throws IOException {
        int maxRetries = properties.getConcurrencyMaxRetries();
        long retryDelayMs = properties.getRaceConditionRetryDelayMs();

        log.debug("Waiting for concurrent request completion: {} ({})", idempotencyKey, operation);

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                TimeUnit.MILLISECONDS.sleep(retryDelayMs);

                Optional<IdempotencyKeyEntity> updated = findExistingResponse(idempotencyKey);
                if (updated.isPresent() && isCompletedResponse(updated.get())) {
                    log.info("Concurrent request completed for key {} ({}) after {} attempts",
                            idempotencyKey, operation, attempt);
                    writeCachedResponse(response, updated.get());
                    return;
                }

                log.debug("Attempt {}/{}: Request still processing for key {} ({})",
                        attempt, maxRetries, idempotencyKey, operation);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for concurrent request: {} ({})", idempotencyKey, operation);
                break;
            }
        }

        log.warn("Timeout after {} attempts waiting for completion: {} ({})", maxRetries, idempotencyKey, operation);
        sendError(response, HttpStatus.CONFLICT,
                "Request is being processed by another instance. Please retry later.");
    }

    private boolean tryAcquireLock(UUID idempotencyKey, String operation) {
        try {
            return transactionTemplate.execute(status -> {
                try {
                    IdempotencyKeyEntity lockEntity = createProcessingLock(idempotencyKey, operation);
                    idempotencyKeyRepository.saveAndFlush(lockEntity);
                    log.debug("Acquired lock for key {} ({})", idempotencyKey, operation);
                    return true;
                } catch (DataIntegrityViolationException e) {
                    log.debug("Lock already exists for key {} ({})", idempotencyKey, operation);
                    return false;
                }
            });
        } catch (Exception e) {
            log.error("Error acquiring lock for key {} ({})", idempotencyKey, operation, e);
            return false;
        }
    }

    private void processAndCacheResponse(HttpServletRequest request,
                                         HttpServletResponse response,
                                         FilterChain filterChain,
                                         UUID idempotencyKey,
                                         String operation) throws IOException, ServletException {

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            log.debug("Processing original request for key {} ({})", idempotencyKey, operation);
            filterChain.doFilter(request, responseWrapper);

            int status = responseWrapper.getStatus();
            byte[] responseBody = responseWrapper.getContentAsByteArray();

            saveFinalResponse(idempotencyKey, status, responseBody, operation);
            log.info("Cached final response (status: {}) for key {} ({})", status, idempotencyKey, operation);

        } finally {
            responseWrapper.copyBodyToResponse();
        }
    }

    private IdempotencyKeyEntity createProcessingLock(UUID key, String operation) {
        IdempotencyKeyEntity entity = new IdempotencyKeyEntity();
        entity.setKey(key);
        entity.setResponseStatus(PROCESSING_STATUS);
        entity.setResponseBody(String.format(
                "{\"status\":\"PROCESSING\",\"message\":\"Request %s is being processed\",\"key\":\"%s\"}",
                operation, key));
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private void saveFinalResponse(UUID key, int status, byte[] responseBody, String operation) {
        try {
            transactionTemplate.executeWithoutResult(txStatus -> {
                IdempotencyKeyEntity entity = idempotencyKeyRepository.findById(key)
                        .orElse(new IdempotencyKeyEntity());

                entity.setKey(key);
                entity.setResponseStatus(status);
                entity.setResponseBody(new String(responseBody, StandardCharsets.UTF_8));
                entity.setCompletedAt(Instant.now());

                idempotencyKeyRepository.save(entity);
            });
        } catch (Exception e) {
            log.error("Error saving final response for key {} ({})", key, operation, e);
        }
    }

    private void releaseLock(UUID key) {
        try {
            transactionTemplate.executeWithoutResult(status ->
                    idempotencyKeyRepository.deleteById(key));
            log.debug("Released lock for key: {}", key);
        } catch (Exception e) {
            log.error("Error releasing lock for key: {}", key, e);
        }
    }

    private Optional<IdempotencyKeyEntity> findExistingResponse(UUID key) {
        try {
            return transactionTemplate.execute(status ->
                    idempotencyKeyRepository.findById(key));
        } catch (Exception e) {
            log.error("Error finding existing response for key: {}", key, e);
            return Optional.empty();
        }
    }

    private boolean isCompletedResponse(IdempotencyKeyEntity entity) {
        return entity.getResponseStatus() != PROCESSING_STATUS && entity.getCompletedAt() != null;
    }

    private boolean isProcessingResponse(IdempotencyKeyEntity entity) {
        return entity.getResponseStatus() == PROCESSING_STATUS && entity.getCompletedAt() == null;
    }

    private void writeCachedResponse(HttpServletResponse response, IdempotencyKeyEntity cached) throws IOException {
        response.setStatus(cached.getResponseStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(cached.getResponseBody());
    }

    private void sendError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String errorJson = String.format(
                "{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d,\"timestamp\":\"%s\"}",
                status.getReasonPhrase(), message, status.value(), LocalDateTime.now()
        );
        response.getWriter().write(errorJson);
    }
}
