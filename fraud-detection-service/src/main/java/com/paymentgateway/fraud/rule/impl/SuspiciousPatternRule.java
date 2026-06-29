package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Rule: SUSPICIOUS_PATTERN</b> — Detects structurally suspicious transaction amounts.
 *
 * <p>This rule looks for two well-known fraud patterns in the transaction amount:
 *
 * <h3>1. Round-amount pattern</h3>
 * <p>Fraudsters often use round amounts (₹10,000, ₹50,000, ₹1,00,000) because they
 * are testing stolen cards or laundering money in clean denominations. Legitimate
 * purchases tend to have "messy" amounts (₹4,799, ₹12,350).
 *
 * <h3>2. Threshold-skirting pattern</h3>
 * <p>Amounts that are <em>just below</em> common reporting or review thresholds
 * (e.g., ₹9,999, ₹49,950) suggest the payer is deliberately trying to stay under
 * the radar. This is a classic structuring / "smurfing" indicator.
 *
 * <p>Each pattern contributes 5 points; if both patterns match, the score is capped at 10.
 */
@Component
public class SuspiciousPatternRule implements FraudRule {

    /** Thresholds that fraudsters commonly try to stay just below. */
    private static final BigDecimal[] COMMON_THRESHOLDS = {
            new BigDecimal("10000"),
            new BigDecimal("25000"),
            new BigDecimal("50000"),
            new BigDecimal("100000"),
            new BigDecimal("200000")
    };

    /** An amount is "just below" a threshold if it's within this margin. */
    private static final BigDecimal THRESHOLD_MARGIN = new BigDecimal("500");

    /** Amounts divisible by this value are considered suspiciously round. */
    private static final BigDecimal ROUND_DIVISOR = new BigDecimal("10000");

    @Override
    public String getName() {
        return "SUSPICIOUS_PATTERN";
    }

    @Override
    public String getDescription() {
        return "Detects round amounts (divisible by ₹10,000) and threshold-skirting patterns (amounts just below common limits).";
    }

    @Override
    public int getMaxScore() {
        return 10;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return FraudRuleResult.clean("Amount not provided or non-positive — skipping pattern check.");
        }

        List<String> findings = new ArrayList<>();
        int score = 0;

        // ── Check 1: Round amount ──────────────────────────────────────
        if (amount.remainder(ROUND_DIVISOR).compareTo(BigDecimal.ZERO) == 0
                && amount.compareTo(ROUND_DIVISOR) >= 0) {
            findings.add(String.format("Round amount detected: ₹%s is divisible by ₹10,000.", amount.toPlainString()));
            score += 5;
        }

        // ── Check 2: Threshold skirting ────────────────────────────────
        for (BigDecimal threshold : COMMON_THRESHOLDS) {
            BigDecimal diff = threshold.subtract(amount);
            // Amount is "just below" if 0 < diff <= margin
            if (diff.compareTo(BigDecimal.ZERO) > 0 && diff.compareTo(THRESHOLD_MARGIN) <= 0) {
                findings.add(String.format("Threshold skirting: ₹%s is just ₹%s below ₹%s limit.",
                        amount.toPlainString(), diff.toPlainString(), threshold.toPlainString()));
                score += 5;
                break; // Only count the closest threshold
            }
        }

        // Cap at max score
        score = Math.min(score, getMaxScore());

        if (score > 0) {
            return FraudRuleResult.flag(score, String.join(" | ", findings));
        }

        return FraudRuleResult.clean(
                String.format("Amount ₹%s does not match any suspicious patterns.", amount.toPlainString()));
    }
}
