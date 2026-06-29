package com.paymentgateway.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Fraud Detection Service — the real-time fraud analysis engine of the Payment Gateway.
 *
 * <p>This service implements a pluggable, rule-based fraud detection architecture that:
 * <ul>
 *   <li>Consumes {@code payment.initiated} events from Kafka</li>
 *   <li>Evaluates each transaction against a pipeline of configurable {@link com.paymentgateway.fraud.rule.FraudRule} beans</li>
 *   <li>Aggregates individual rule scores into a composite risk score (0–100)</li>
 *   <li>Classifies the transaction into a {@link com.paymentgateway.common.enums.RiskLevel}</li>
 *   <li>Publishes the result on the {@code fraud.check.result} Kafka topic</li>
 * </ul>
 *
 * <p>Adding a new fraud rule is as simple as creating a Spring {@code @Component} that
 * implements {@link com.paymentgateway.fraud.rule.FraudRule}. The engine discovers all
 * rule beans automatically via dependency injection — <strong>zero configuration required</strong>.
 *
 * @author Payment Gateway Team
 */
@SpringBootApplication
@EnableJpaAuditing
public class FraudDetectionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionServiceApplication.class, args);
    }
}
