package com.fintrack.transaction_service.dto;

import com.fintrack.transaction_service.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDtos {

    @Data
    public static class CreateTransactionRequest {

        @NotNull(message = "Account ID is required")
        private Long accountId;

        @NotNull(message = "Transaction type is required")
        private TransactionType type;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        @NotBlank(message = "Description is required")
        @Size(max = 255, message = "Description must be less than 255 characters")
        private String description;

        @Size(max = 100, message = "Category must be less than 100 characters")
        private String category;

        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        private String currency;
    }

    @Data
    @Builder
    public static class TransactionResponse {
        private Long id;
        private Long userId;
        private Long accountId;
        private TransactionType type;
        private BigDecimal amount;
        private String description;
        private String category;
        private String currency;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    public static class PageResponse<T> {
        private java.util.List<T> content;
        private int pageNumber;
        private int pageSize;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String path;
        private String timestamp;
    }
}