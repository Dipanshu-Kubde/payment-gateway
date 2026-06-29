package com.paymentgateway.fraud.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result returned by a single {@link FraudRule} evaluation.
 *
 * <p>Each rule evaluates a transaction and produces a result that indicates:
 * <ul>
 *   <li><b>triggered</b> — whether the rule detected suspicious behaviour</li>
 *   <li><b>score</b> — the risk-score contribution (0 if not triggered, up to {@code maxScore} if triggered)</li>
 *   <li><b>details</b> — a human-readable explanation of why the rule fired (or why it didn't)</li>
 * </ul>
 *
 * <p>The fraud engine sums the {@code score} values from all rules to compute the
 * composite risk score for the transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudRuleResult {

    /** {@code true} if the rule detected a suspicious signal. */
    private boolean triggered;

    /** Risk-score contribution. Always 0 when {@code triggered} is false. */
    private int score;

    /** Human-readable explanation (e.g. "Amount ₹75,000 exceeds ₹50,000 threshold"). */
    private String details;

    // ── Convenience factory methods ──────────────────────────────────────

    /** Create a "clean" result — the rule did not fire. */
    public static FraudRuleResult clean(String details) {
        return FraudRuleResult.builder()
                .triggered(false)
                .score(0)
                .details(details)
                .build();
    }

    /** Create a "triggered" result with the given score contribution. */
    public static FraudRuleResult flag(int score, String details) {
        return FraudRuleResult.builder()
                .triggered(true)
                .score(score)
                .details(details)
                .build();
    }
}
