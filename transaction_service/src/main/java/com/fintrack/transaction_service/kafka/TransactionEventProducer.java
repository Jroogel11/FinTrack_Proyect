package com.fintrack.transaction_service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private static final String TOPIC = "fintrack.transactions";

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    public void sendTransactionCreated(TransactionEvent event) {
        event.setOccurredAt(LocalDateTime.now());

        kafkaTemplate.send(TOPIC, event.getAccountId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Transaction event sent — topic: {}, accountId: {}, type: {}, amount: {}",
                                TOPIC,
                                event.getAccountId(),
                                event.getType(),
                                event.getAmount());
                    } else {
                        log.error("Failed to send transaction event — accountId: {}, error: {}",
                                event.getAccountId(),
                                ex.getMessage());
                    }
                });
    }
}