package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * <b>Rule: VELOCITY_CHECK</b> — Detects rapid-fire transactions from the same customer.
 *
 * <p>Fraudsters often attempt multiple transactions in quick succession to exploit
 * stolen credentials before an account is locked. This rule uses a Redis-backed
 * sliding window to count how many transactions a given customer email has initiated
 * within the last <b>10 minutes</b>.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>A Redis key {@code fraud:velocity:{email}} is incremented for each transaction.</li>
 *   <li>The key's TTL is set to 10 minutes (only on first creation).</li>
 *   <li>If the count exceeds <b>5</b>, the rule fires with score 20.</li>
 * </ol>
 *
 * <p><b>Degradation:</b> If Redis is unavailable, the rule logs a warning and
 * returns a clean result to avoid blocking legitimate transactions.
 */
@Slf4j
@Component
public class VelocityCheckRule implements FraudRule {

    private static final String KEY_PREFIX = "fraud:velocity:";
    private static final int WINDOW_MINUTES = 10;
    private static final long MAX_TRANSACTIONS_IN_WINDOW = 5;

    private final StringRedisTemplate redisTemplate;

    public VelocityCheckRule(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getName() {
        return "VELOCITY_CHECK";
    }

    @Override
    public String getDescription() {
        return "Flags customers who initiate more than 5 transactions within a 10-minute sliding window.";
    }

    @Override
    public int getMaxScore() {
        return 20;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        if (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank()) {
            return FraudRuleResult.clean("Customer email not available — skipping velocity check.");
        }

        try {
            String key = KEY_PREFIX + request.getCustomerEmail().toLowerCase();
            Long count = redisTemplate.opsForValue().increment(key);

            // Set expiry only on the first increment (when count == 1)
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofMinutes(WINDOW_MINUTES));
            }

            if (count != null && count > MAX_TRANSACTIONS_IN_WINDOW) {
                return FraudRuleResult.flag(20,
                        String.format("High velocity: %d transactions in the last %d minutes (threshold: %d).",
                                count, WINDOW_MINUTES, MAX_TRANSACTIONS_IN_WINDOW));
            }

            return FraudRuleResult.clean(
                    String.format("Transaction count (%d) within acceptable range for window.", count));

        } catch (Exception e) {
            log.warn("Redis unavailable for velocity check — degrading gracefully. Error: {}", e.getMessage());
            return FraudRuleResult.clean("Velocity check skipped — Redis unavailable.");
        }
    }
}
