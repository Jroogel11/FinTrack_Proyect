package com.fintrack.transaction_service.service;

import com.fintrack.transaction_service.dto.TransactionDtos.*;
import com.fintrack.transaction_service.entity.Transaction;
import com.fintrack.transaction_service.kafka.TransactionEvent;
import com.fintrack.transaction_service.kafka.TransactionEventProducer;
import com.fintrack.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    @Transactional
    public TransactionResponse createTransaction(
            CreateTransactionRequest request, Long userId) {

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .accountId(request.getAccountId())
                .type(request.getType())
                .amount(request.getAmount())
                .description(request.getDescription())
                .category(request.getCategory())
                .currency(request.getCurrency() != null
                        ? request.getCurrency()
                        : "EUR")
                .build();

        Transaction saved = transactionRepository.save(transaction);

        eventProducer.sendTransactionCreated(TransactionEvent.builder()
                .transactionId(saved.getId())
                .accountId(saved.getAccountId())
                .userId(saved.getUserId())
                .type(saved.getType())
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .build());

        log.info("Transaction created — id: {}, accountId: {}, type: {}, amount: {}",
                saved.getId(),
                saved.getAccountId(),
                saved.getType(),
                saved.getAmount());

        return toResponse(saved);
    }

    public PageResponse<TransactionResponse> getUserTransactions(
            Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> result = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return toPageResponse(result);
    }

    public PageResponse<TransactionResponse> getAccountTransactions(
            Long accountId, Long userId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> result = transactionRepository
                .findByAccountIdAndUserIdOrderByCreatedAtDesc(accountId, userId, pageable);

        return toPageResponse(result);
    }

    public TransactionResponse getTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(t -> t.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return toResponse(transaction);
    }

    private PageResponse<TransactionResponse> toPageResponse(Page<Transaction> page) {
        return PageResponse.<TransactionResponse>builder()
                .content(page.getContent()
                        .stream()
                        .map(this::toResponse)
                        .collect(Collectors.toList()))
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }

    private TransactionResponse toResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .accountId(transaction.getAccountId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .category(transaction.getCategory())
                .currency(transaction.getCurrency())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}