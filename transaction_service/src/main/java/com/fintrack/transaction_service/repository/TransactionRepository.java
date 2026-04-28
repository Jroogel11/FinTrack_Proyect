package com.fintrack.transaction_service.repository;

import com.fintrack.transaction_service.entity.Transaction;
import com.fintrack.transaction_service.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Transaction> findByAccountIdAndUserIdOrderByCreatedAtDesc(
            Long accountId, Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeOrderByCreatedAtDesc(
            Long userId, TransactionType type, Pageable pageable);
}