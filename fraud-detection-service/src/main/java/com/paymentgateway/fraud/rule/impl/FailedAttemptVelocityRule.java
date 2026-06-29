package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <b>Rule: FAILED_ATTEMPT_VELOCITY</b> — Detects repeated failed payment attempts.
 *
 * <p>Multiple failed attempts in a short period are a strong indicator of card testing
 * or brute-force attacks. This rule uses Redis to track the number of failed payment
 * attempts per customer email within a <b>5-minute</b> window.
 *
 * <h3>Integration note</h3>
 * <p>Failed attempts are tracked via the Redis key {@code fraud:failed:{email}}.
 * The Payment Service is expected to call {@code recordFailedAttempt(email)} (or
 * increment the Redis counter directly) whenever a payment fails. This rule reads
 * the counter at evaluation time.
 *
 * <p>If no counter exists, the rule assumes zero prior failures and returns clean.
 */
@Slf4j
@Component
public class FailedAttemptVelocityRule implements FraudRule {

    private static final String KEY_PREFIX = "fraud:failed:";
    private static final int WINDOW_MINUTES = 5;
    private static final long MAX_FAILED_ATTEMPTS = 3;

    private final StringRedisTemplate redisTemplate;

    public FailedAttemptVelocityRule(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getName() {
        return "FAILED_ATTEMPT_VELOCITY";
    }

    @Override
    public String getDescription() {
        return "Flags customers with more than 3 failed payment attempts within a 5-minute window.";
    }

    @Override
    public int getMaxScore() {
        return 15;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        if (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank()) {
            return FraudRuleResult.clean("Customer email not available — skipping failed attempt check.");
        }

        try {
            String key = KEY_PREFIX + request.getCustomerEmail().toLowerCase();
            String value = redisTemplate.opsForValue().get(key);

            // If no counter exists, record this as a potential first attempt
            if (value == null) {
                // Initialize the counter for tracking — subsequent failures will increment it
                redisTemplate.opsForValue().set(key, "0", Duration.ofMinutes(WINDOW_MINUTES));
                return FraudRuleResult.clean("No prior failed attempts detected.");
            }

            long failedCount = Long.parseLong(value);

            if (failedCount > MAX_FAILED_ATTEMPTS) {
                return FraudRuleResult.flag(15,
                        String.format("Suspicious: %d failed attempts in the last %d minutes (threshold: %d).",
                                failedCount, WINDOW_MINUTES, MAX_FAILED_ATTEMPTS));
            }

            return FraudRuleResult.clean(
                    String.format("Failed attempt count (%d) is within acceptable range.", failedCount));

        } catch (Exception e) {
            log.warn("Redis unavailable for failed-attempt check — degrading gracefully. Error: {}", e.getMessage());
            return FraudRuleResult.clean("Failed attempt check skipped — Redis unavailable.");
        }
    }
}
