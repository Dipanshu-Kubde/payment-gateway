package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * <b>Rule: HIGH_AMOUNT</b> — Flags transactions with unusually large amounts.
 *
 * <p>Large one-time transactions are a classic fraud vector. This rule assigns a
 * graduated risk score based on the transaction amount:
 * <ul>
 *   <li>Amount &gt; ₹50,000 → score <b>10</b></li>
 *   <li>Amount &gt; ₹1,00,000 → score <b>15</b> (maximum)</li>
 * </ul>
 *
 * <p>The graduated scoring avoids a hard cut-off and gives the composite engine
 * a more nuanced signal.
 */
@Component
public class HighAmountRule implements FraudRule {

    private static final BigDecimal THRESHOLD_MEDIUM = new BigDecimal("50000");
    private static final BigDecimal THRESHOLD_HIGH = new BigDecimal("100000");

    @Override
    public String getName() {
        return "HIGH_AMOUNT";
    }

    @Override
    public String getDescription() {
        return "Flags transactions exceeding ₹50,000. Higher amounts receive progressively higher risk scores.";
    }

    @Override
    public int getMaxScore() {
        return 15;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        BigDecimal amount = request.getAmount();
        if (amount == null) {
            return FraudRuleResult.clean("Amount not provided — skipping rule.");
        }

        if (amount.compareTo(THRESHOLD_HIGH) > 0) {
            return FraudRuleResult.flag(15,
                    String.format("Very high amount: ₹%s exceeds ₹1,00,000 threshold.", amount.toPlainString()));
        }

        if (amount.compareTo(THRESHOLD_MEDIUM) > 0) {
            return FraudRuleResult.flag(10,
                    String.format("High amount: ₹%s exceeds ₹50,000 threshold.", amount.toPlainString()));
        }

        return FraudRuleResult.clean(
                String.format("Amount ₹%s is within normal range.", amount.toPlainString()));
    }
}
