package com.fintrack.account_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountEventProducer {

    private static final String TOPIC = "fintrack.accounts";

    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;

    public void sendAccountCreated(AccountEvent event) {
        event.setEventType("ACCOUNT_CREATED");
        event.setOccurredAt(LocalDateTime.now());
        send(event);
    }

    public void sendBalanceUpdated(AccountEvent event) {
        event.setEventType("BALANCE_UPDATED");
        event.setOccurredAt(LocalDateTime.now());
        send(event);
    }

    private void send(AccountEvent event) {
        kafkaTemplate.send(TOPIC, event.getAccountId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Event sent to Kafka — topic: {}, key: {}, eventType: {}",
                                TOPIC,
                                event.getAccountId(),
                                event.getEventType());
                    } else {
                        log.error("Failed to send event to Kafka — topic: {}, eventType: {}, error: {}",
                                TOPIC,
                                event.getEventType(),
                                ex.getMessage());
                    }
                });
    }
}