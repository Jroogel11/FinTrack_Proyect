package com.fintrack.transaction_service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountEventConsumer {

    @KafkaListener(
        topics = "fintrack.accounts",
        groupId = "transaction-service-group"
    )
    public void handleAccountEvent(AccountEvent event) {
        log.info("Received account event — accountId: {}, eventType: {}, balance: {}",
                event.getAccountId(),
                event.getEventType(),
                event.getBalance());

        if ("ACCOUNT_CREATED".equals(event.getEventType())) {
            handleAccountCreated(event);
        } else if ("BALANCE_UPDATED".equals(event.getEventType())) {
            handleBalanceUpdated(event);
        } else {
            log.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void handleAccountCreated(AccountEvent event) {
        log.info("Account available for transactions — accountId: {}, userId: {}, balance: {}",
                event.getAccountId(),
                event.getUserId(),
                event.getBalance());
    }

    private void handleBalanceUpdated(AccountEvent event) {
        log.info("Account balance updated — accountId: {}, newBalance: {}",
                event.getAccountId(),
                event.getBalance());
    }
}