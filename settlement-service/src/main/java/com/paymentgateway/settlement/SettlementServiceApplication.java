package com.paymentgateway.settlement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Settlement Service Application
 *
 * Handles merchant settlement processing, balance tracking, and automated payouts.
 * Consumes payment.processed events to track merchant balances and runs daily
 * settlement batches to disburse funds to merchants.
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class SettlementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SettlementServiceApplication.class, args);
    }
}
