package com.paymentgateway.fraud.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable context object that carries all the information a {@link FraudRule}
 * needs to evaluate a single transaction.
 *
 * <p>This object is built once from the incoming {@code PaymentInitiatedEvent} and
 * then passed to every rule in the pipeline. Rules should treat it as read-only.
 *
 * <h3>Field reference</h3>
 * <table>
 *   <tr><th>Field</th><th>Used by</th></tr>
 *   <tr><td>amount</td><td>HighAmountRule, FirstTimeHighAmountRule, SuspiciousPatternRule</td></tr>
 *   <tr><td>customerEmail</td><td>VelocityCheckRule, FailedAttemptVelocityRule, FirstTimeHighAmountRule</td></tr>
 *   <tr><td>customerIp</td><td>GeoLocationMismatchRule, HighRiskCountryRule</td></tr>
 *   <tr><td>cardBinCountry</td><td>GeoLocationMismatchRule</td></tr>
 *   <tr><td>timestamp</td><td>UnusualHourRule</td></tr>
 * </table>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudCheckRequest {

    private String transactionId;
    private String orderId;
    private Long merchantId;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String customerEmail;
    private String customerIp;
    private String cardBinCountry;
    private LocalDateTime timestamp;
}
