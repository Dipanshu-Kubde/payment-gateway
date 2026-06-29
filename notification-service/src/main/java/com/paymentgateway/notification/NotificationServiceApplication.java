package com.paymentgateway.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Notification Service Application
 *
 * Handles dispatching notifications (email, SMS, webhook) triggered by
 * payment lifecycle events consumed from Kafka topics:
 * - payment.processed   → success/failure notifications to customers
 * - fraud.check.result  → fraud alert notifications for high-risk transactions
 * - settlement.completed → settlement summary notifications to merchants
 */
@SpringBootApplication
@EnableJpaAuditing
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
