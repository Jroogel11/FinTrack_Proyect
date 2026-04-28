package com.fintrack.account_service.kafka;

import com.fintrack.account_service.kafka.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {

    private Long transactionId;
    private Long accountId;
    private Long userId;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private LocalDateTime occurredAt;
}