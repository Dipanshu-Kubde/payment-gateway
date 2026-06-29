package com.paymentgateway.settlement.kafka;

import com.paymentgateway.common.event.SettlementEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes settlement completion events to the "settlement.completed" Kafka topic.
 * Consumed by the Notification Service to alert merchants of payouts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventPublisher {

    private static final String TOPIC = "settlement.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes a SettlementEvent to Kafka, keyed by merchantId for partition affinity.
     *
     * @param event the settlement event to publish
     */
    public void publishSettlementEvent(SettlementEvent event) {
        String key = String.valueOf(event.getMerchantId());

        log.info("Publishing settlement event to topic '{}': settlementId={}, merchantId={}, status={}",
                TOPIC, event.getSettlementId(), event.getMerchantId(), event.getStatus());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(TOPIC, key, event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish settlement event for settlementId={}: {}",
                        event.getSettlementId(), ex.getMessage(), ex);
            } else {
                log.info("Settlement event published successfully: settlementId={}, partition={}, offset={}",
                        event.getSettlementId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
