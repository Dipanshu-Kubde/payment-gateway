package com.paymentgateway.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Payment Service - Core payment processing engine.
 *
 * Responsibilities:
 * - Create and manage payment orders
 * - Process payments with fraud detection integration
 * - Handle payment retries with exponential backoff
 * - Publish payment events to Kafka for downstream services
 * - Idempotency enforcement via Redis
 */
@SpringBootApplication(scanBasePackages = {
        "com.paymentgateway.payment",
        "com.paymentgateway.common"
})
@EnableJpaAuditing
@EnableScheduling
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
