package com.fintrack.account_service.dto;

import com.fintrack.account_service.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountDtos {

    @Data
    public static class CreateAccountRequest {

        @NotBlank(message = "Account name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;

        @NotNull(message = "Account type is required")
        private AccountType type;

        private BigDecimal initialBalance;

        @Size(min = 3, max = 3, message = "Currency must be 3 characters")
        private String currency;
    }

    @Data
    public static class UpdateAccountRequest {

        @NotBlank(message = "Account name is required")
        @Size(min = 2, max = 100)
        private String name;
    }

    @Data
    @Builder
    public static class AccountResponse {
        private Long id;
        private Long userId;
        private String name;
        private AccountType type;
        private BigDecimal balance;
        private String currency;
        private boolean active;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
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