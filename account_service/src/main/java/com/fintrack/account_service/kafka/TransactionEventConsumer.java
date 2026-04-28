package com.fintrack.account_service.kafka;

import com.fintrack.account_service.entity.Account;
import com.fintrack.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    @KafkaListener(
        topics = "fintrack.transactions",
        groupId = "account-service-group"
    )
    @Transactional
    public void handleTransactionEvent(TransactionEvent event) {
        log.info("Received transaction event — transactionId: {}, accountId: {}, type: {}, amount: {}",
                event.getTransactionId(),
                event.getAccountId(),
                event.getType(),
                event.getAmount());

        Account account = accountRepository.findById(event.getAccountId())
                .orElse(null);

        if (account == null) {
            log.warn("Account not found for transaction event — accountId: {}",
                    event.getAccountId());
            return;
        }

        BigDecimal currentBalance = account.getBalance();
        BigDecimal newBalance;

        if (TransactionType.INCOME.equals(event.getType())) {
            newBalance = currentBalance.add(event.getAmount());
        } else {
            newBalance = currentBalance.subtract(event.getAmount());
        }

        account.setBalance(newBalance);
        accountRepository.save(account);

        log.info("Balance updated — accountId: {}, oldBalance: {}, newBalance: {}",
                account.getId(),
                currentBalance,
                newBalance);

        eventProducer.sendBalanceUpdated(AccountEvent.builder()
                .accountId(account.getId())
                .userId(account.getUserId())
                .accountName(account.getName())
                .balance(newBalance)
                .build());
    }
}