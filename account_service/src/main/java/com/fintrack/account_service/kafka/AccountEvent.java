package com.fintrack.account_service.kafka;

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
public class AccountEvent {

    private Long accountId;
    private Long userId;
    private String accountName;
    private BigDecimal balance;
    private String eventType;
    private LocalDateTime occurredAt;
}