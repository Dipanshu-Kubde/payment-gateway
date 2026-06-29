package com.paymentgateway.fraud.rule;

/**
 * Contract for a single fraud detection rule.
 *
 * <p><strong>How to add a new rule:</strong></p>
 * <ol>
 *   <li>Create a new class that implements this interface.</li>
 *   <li>Annotate it with {@code @Component} (or any Spring stereotype).</li>
 *   <li>Implement the four methods below.</li>
 *   <li>That's it — the fraud engine discovers the bean automatically.</li>
 * </ol>
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li><b>Single Responsibility:</b> each rule checks exactly one fraud signal.</li>
 *   <li><b>Stateless evaluation:</b> all context is provided via {@link FraudCheckRequest}.</li>
 *   <li><b>Bounded scoring:</b> a rule's score contribution must never exceed {@link #getMaxScore()}.</li>
 *   <li><b>Fail-safe:</b> if a rule encounters an error, it should return {@code FraudRuleResult.clean()} rather than throw.</li>
 * </ul>
 *
 * @see FraudRuleResult
 * @see FraudCheckRequest
 */
public interface FraudRule {

    /**
     * A short, unique, UPPER_SNAKE_CASE identifier for this rule.
     * Used in logs, triggered-rules lists, and the admin dashboard.
     * Example: {@code "HIGH_AMOUNT"}, {@code "VELOCITY_CHECK"}
     */
    String getName();

    /**
     * Human-readable description of what this rule checks.
     * Displayed in the active-rules API endpoint.
     */
    String getDescription();

    /**
     * Maximum risk-score contribution this rule can assign to a single transaction.
     * The fraud engine uses this for transparency and auditing.
     *
     * @return a positive integer (typically 5–25)
     */
    int getMaxScore();

    /**
     * Evaluate the given transaction against this rule.
     *
     * @param request the transaction context (never {@code null})
     * @return a {@link FraudRuleResult} indicating whether the rule fired and its score
     */
    FraudRuleResult evaluate(FraudCheckRequest request);
}
