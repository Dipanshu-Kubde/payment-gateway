package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * <b>Rule: FIRST_TIME_HIGH_AMOUNT</b> — Flags first-time customers making large transactions.
 *
 * <p>A brand-new customer immediately making a high-value transaction (above ₹20,000)
 * is suspicious. Legitimate customers typically start with smaller purchases to test
 * a new platform. This pattern is commonly seen with stolen card details being used
 * for a single high-value purchase.
 *
 * <h3>How customer history is tracked</h3>
 * <p>The rule maintains a Redis key {@code fraud:customer-history:{email}} that is
 * set after a customer's first transaction. On subsequent evaluations, the existence
 * of this key indicates the customer has prior history, and the rule returns clean.
 *
 * <p>The key has a <b>90-day TTL</b> — after 3 months of inactivity, the customer
 * is treated as "new" again.
 */
@Slf4j
@Component
public class FirstTimeHighAmountRule implements FraudRule {

    private static final String KEY_PREFIX = "fraud:customer-history:";
    private static final BigDecimal FIRST_TIME_THRESHOLD = new BigDecimal("20000");
    private static final long HISTORY_TTL_DAYS = 90;

    private final StringRedisTemplate redisTemplate;

    public FirstTimeHighAmountRule(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String getName() {
        return "FIRST_TIME_HIGH_AMOUNT";
    }

    @Override
    public String getDescription() {
        return "Flags first-time customers who make transactions exceeding ₹20,000 — a common pattern with stolen credentials.";
    }

    @Override
    public int getMaxScore() {
        return 10;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        if (request.getCustomerEmail() == null || request.getCustomerEmail().isBlank()) {
            return FraudRuleResult.clean("Customer email not available — skipping first-time check.");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null) {
            return FraudRuleResult.clean("Amount not provided — skipping first-time check.");
        }

        try {
            String key = KEY_PREFIX + request.getCustomerEmail().toLowerCase();
            Boolean hasHistory = redisTemplate.hasKey(key);

            if (Boolean.TRUE.equals(hasHistory)) {
                return FraudRuleResult.clean("Returning customer — first-time rule does not apply.");
            }

            // Mark this customer as having history for future evaluations
            redisTemplate.opsForValue().set(key, "1",
                    java.time.Duration.ofDays(HISTORY_TTL_DAYS));

            if (amount.compareTo(FIRST_TIME_THRESHOLD) > 0) {
                return FraudRuleResult.flag(10,
                        String.format("First-time customer attempting high-value transaction: ₹%s (threshold: ₹20,000).",
                                amount.toPlainString()));
            }

            return FraudRuleResult.clean(
                    String.format("First-time customer, but amount ₹%s is below ₹20,000 threshold.",
                            amount.toPlainString()));

        } catch (Exception e) {
            log.warn("Redis unavailable for first-time check — degrading gracefully. Error: {}", e.getMessage());
            return FraudRuleResult.clean("First-time check skipped — Redis unavailable.");
        }
    }
}
