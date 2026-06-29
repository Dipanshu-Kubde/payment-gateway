package com.paymentgateway.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed idempotency service.
 * Prevents duplicate payment processing by storing idempotency keys in Redis with a 24-hour TTL.
 * Uses Redis SETNX (setIfAbsent) for atomic check-and-set operations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyService {

    private static final String IDEMPOTENCY_PREFIX = "idempotency:";
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * Checks if a transaction with the given idempotency key has already been processed.
     * Uses SETNX to atomically set the key only if it doesn't exist.
     *
     * @param key the idempotency key
     * @return true if this key has already been seen (duplicate), false if new
     */
    public boolean isDuplicate(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String redisKey = IDEMPOTENCY_PREFIX + key;
        Boolean wasSet = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", IDEMPOTENCY_TTL);
        boolean duplicate = Boolean.FALSE.equals(wasSet);
        if (duplicate) {
            log.warn("Duplicate idempotency key detected: {}", key);
        }
        return duplicate;
    }

    /**
     * Stores the result associated with an idempotency key.
     * Updates the value while preserving the existing TTL.
     *
     * @param key    the idempotency key
     * @param result the result to cache (e.g., serialized PaymentResponse)
     */
    public void storeResult(String key, String result) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = IDEMPOTENCY_PREFIX + key;
        redisTemplate.opsForValue().set(redisKey, result, IDEMPOTENCY_TTL);
        log.debug("Stored idempotency result for key: {}", key);
    }

    /**
     * Retrieves the cached result for a previously processed idempotency key.
     *
     * @param key the idempotency key
     * @return the cached result, or null if not found
     */
    public String getCachedResult(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String redisKey = IDEMPOTENCY_PREFIX + key;
        return redisTemplate.opsForValue().get(redisKey);
    }
}
