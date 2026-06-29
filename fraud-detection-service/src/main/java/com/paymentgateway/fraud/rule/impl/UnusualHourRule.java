package com.paymentgateway.fraud.rule.impl;

import com.paymentgateway.fraud.rule.FraudCheckRequest;
import com.paymentgateway.fraud.rule.FraudRule;
import com.paymentgateway.fraud.rule.FraudRuleResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * <b>Rule: UNUSUAL_HOUR</b> — Flags transactions initiated during off-peak hours.
 *
 * <p>Legitimate transactions peak during business hours. Transactions between
 * <b>2:00 AM and 5:00 AM</b> (server time) are statistically more likely to be
 * fraudulent, as they often correlate with automated bots or stolen credentials
 * being used while the real cardholder is asleep.
 *
 * <p>This is a low-confidence signal (max score = 5), meaning it won't block a
 * transaction on its own but will contribute to the composite risk score when
 * combined with other suspicious signals.
 */
@Component
public class UnusualHourRule implements FraudRule {

    private static final LocalTime SUSPICIOUS_START = LocalTime.of(2, 0);
    private static final LocalTime SUSPICIOUS_END = LocalTime.of(5, 0);

    @Override
    public String getName() {
        return "UNUSUAL_HOUR";
    }

    @Override
    public String getDescription() {
        return "Flags transactions initiated between 2:00 AM and 5:00 AM — a low-confidence but additive signal.";
    }

    @Override
    public int getMaxScore() {
        return 5;
    }

    @Override
    public FraudRuleResult evaluate(FraudCheckRequest request) {
        LocalDateTime timestamp = request.getTimestamp();
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }

        LocalTime txnTime = timestamp.toLocalTime();

        if (!txnTime.isBefore(SUSPICIOUS_START) && txnTime.isBefore(SUSPICIOUS_END)) {
            return FraudRuleResult.flag(5,
                    String.format("Transaction initiated at unusual hour: %s (flagged window: 02:00–05:00).", txnTime));
        }

        return FraudRuleResult.clean(
                String.format("Transaction time %s is within normal operating hours.", txnTime));
    }
}
