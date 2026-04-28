package com.fintrack.account_service.service;

import com.fintrack.account_service.dto.AccountDtos.*;
import com.fintrack.account_service.entity.Account;
import com.fintrack.account_service.kafka.AccountEvent;
import com.fintrack.account_service.kafka.AccountEventProducer;
import com.fintrack.account_service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountEventProducer eventProducer;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request, Long userId) {

        Account account = Account.builder()
                .userId(userId)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getInitialBalance() != null
                        ? request.getInitialBalance()
                        : BigDecimal.ZERO)
                .currency(request.getCurrency() != null
                        ? request.getCurrency()
                        : "EUR")
                .active(true)
                .build();

        Account saved = accountRepository.save(account);

        eventProducer.sendAccountCreated(AccountEvent.builder()
                .accountId(saved.getId())
                .userId(saved.getUserId())
                .accountName(saved.getName())
                .balance(saved.getBalance())
                .build());

        log.info("Account created: id={}, userId={}", saved.getId(), saved.getUserId());
        return toResponse(saved);
    }

    public List<AccountResponse> getUserAccounts(Long userId) {
        return accountRepository.findByUserIdAndActiveTrue(userId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
        return toResponse(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long accountId, UpdateAccountRequest request, Long userId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setName(request.getName());
        Account updated = accountRepository.save(account);

        log.info("Account updated: id={}", updated.getId());
        return toResponse(updated);
    }

    @Transactional
    public void deleteAccount(Long accountId, Long userId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        account.setActive(false);
        accountRepository.save(account);

        log.info("Account deactivated: id={}", accountId);
    }

    public boolean accountBelongsToUser(Long accountId, Long userId) {
        return accountRepository.existsByIdAndUserId(accountId, userId);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .active(account.isActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}