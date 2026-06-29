package com.paymentgateway.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Transaction Service - Records and provides analytics for all payment transactions.
 *
 * <p>Consumes Kafka events from Payment Service to create and update transaction records.
 * Exposes REST APIs for querying transactions, statistics, and analytics data.</p>
 */
@SpringBootApplication
@EnableJpaAuditing
public class TransactionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransactionServiceApplication.class, args);
    }
}
