package com.fintrack.account_service.service;

import com.fintrack.account_service.dto.AccountDtos.*;
import com.fintrack.account_service.entity.Account;
import com.fintrack.account_service.entity.AccountType;
import com.fintrack.account_service.kafka.AccountEvent;
import com.fintrack.account_service.kafka.AccountEventProducer;
import com.fintrack.account_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountEventProducer eventProducer;

    @InjectMocks
    private AccountService accountService;

    private Account testAccount;
    private final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        testAccount = Account.builder()
                .id(1L)
                .userId(USER_ID)
                .name("Cuenta Corriente")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("EUR")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────────────
    // CREATE ACCOUNT
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create account successfully with initial balance")
    void should_create_account_with_initial_balance() {
        // ARRANGE
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Cuenta Corriente");
        request.setType(AccountType.CHECKING);
        request.setInitialBalance(new BigDecimal("1000.00"));
        request.setCurrency("EUR");

        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        doNothing().when(eventProducer).sendAccountCreated(any(AccountEvent.class));

        // ACT
        AccountResponse response = accountService.createAccount(request, USER_ID);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Cuenta Corriente");
        assertThat(response.getBalance()).isEqualByComparingTo("1000.00");
        assertThat(response.getCurrency()).isEqualTo("EUR");
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.isActive()).isTrue();

        verify(accountRepository, times(1)).save(any(Account.class));
        verify(eventProducer, times(1)).sendAccountCreated(any(AccountEvent.class));
    }

    @Test
    @DisplayName("Should create account with zero balance when no initial balance provided")
    void should_create_account_with_zero_balance_when_no_initial_balance() {
        // ARRANGE
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Cuenta Sin Saldo");
        request.setType(AccountType.SAVINGS);

        Account accountWithZeroBalance = Account.builder()
                .id(2L)
                .userId(USER_ID)
                .name("Cuenta Sin Saldo")
                .type(AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .currency("EUR")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountRepository.save(any(Account.class))).thenReturn(accountWithZeroBalance);
        doNothing().when(eventProducer).sendAccountCreated(any(AccountEvent.class));

        // ACT
        AccountResponse response = accountService.createAccount(request, USER_ID);

        // ASSERT
        assertThat(response.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getCurrency()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should use EUR as default currency when not provided")
    void should_use_eur_as_default_currency() {
        // ARRANGE
        CreateAccountRequest request = new CreateAccountRequest();
        request.setName("Cuenta");
        request.setType(AccountType.CHECKING);

        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> {
            Account saved = inv.getArgument(0);
            assertThat(saved.getCurrency()).isEqualTo("EUR");
            return testAccount;
        });
        doNothing().when(eventProducer).sendAccountCreated(any());

        // ACT
        accountService.createAccount(request, USER_ID);

        // ASSERT — verificado dentro del thenAnswer
        verify(accountRepository).save(any(Account.class));
    }

    // ─────────────────────────────────────────────────────
    // GET ACCOUNTS
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all active accounts for user")
    void should_return_active_accounts_for_user() {
        // ARRANGE
        Account secondAccount = Account.builder()
                .id(2L)
                .userId(USER_ID)
                .name("Cuenta Ahorro")
                .type(AccountType.SAVINGS)
                .balance(new BigDecimal("500.00"))
                .currency("EUR")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountRepository.findByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of(testAccount, secondAccount));

        // ACT
        List<AccountResponse> accounts = accountService.getUserAccounts(USER_ID);

        // ASSERT
        assertThat(accounts).hasSize(2);
        assertThat(accounts.get(0).getName()).isEqualTo("Cuenta Corriente");
        assertThat(accounts.get(1).getName()).isEqualTo("Cuenta Ahorro");
    }

    @Test
    @DisplayName("Should return empty list when user has no accounts")
    void should_return_empty_list_when_no_accounts() {
        // ARRANGE
        when(accountRepository.findByUserIdAndActiveTrue(USER_ID))
                .thenReturn(List.of());

        // ACT
        List<AccountResponse> accounts = accountService.getUserAccounts(USER_ID);

        // ASSERT
        assertThat(accounts).isEmpty();
    }

    @Test
    @DisplayName("Should return account when it belongs to user")
    void should_return_account_when_belongs_to_user() {
        // ARRANGE
        when(accountRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(testAccount));

        // ACT
        AccountResponse response = accountService.getAccount(1L, USER_ID);

        // ASSERT
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should throw exception when account not found or belongs to another user")
    void should_throw_exception_when_account_not_found() {
        // ARRANGE
        when(accountRepository.findByIdAndUserId(99L, USER_ID))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> accountService.getAccount(99L, USER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ─────────────────────────────────────────────────────
    // UPDATE ACCOUNT
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update account name successfully")
    void should_update_account_name() {
        // ARRANGE
        UpdateAccountRequest request = new UpdateAccountRequest();
        request.setName("Nuevo Nombre");

        Account updatedAccount = Account.builder()
                .id(1L)
                .userId(USER_ID)
                .name("Nuevo Nombre")
                .type(AccountType.CHECKING)
                .balance(new BigDecimal("1000.00"))
                .currency("EUR")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(accountRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenReturn(updatedAccount);

        // ACT
        AccountResponse response = accountService.updateAccount(1L, request, USER_ID);

        // ASSERT
        assertThat(response.getName()).isEqualTo("Nuevo Nombre");
        verify(accountRepository, times(1)).save(any(Account.class));
    }

    // ─────────────────────────────────────────────────────
    // DELETE ACCOUNT (SOFT DELETE)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should deactivate account instead of deleting it")
    void should_deactivate_account_on_delete() {
        // ARRANGE
        when(accountRepository.findByIdAndUserId(1L, USER_ID))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        accountService.deleteAccount(1L, USER_ID);

        // ASSERT — la cuenta se marcó como inactiva, no se borró
        assertThat(testAccount.isActive()).isFalse();
        verify(accountRepository, times(1)).save(testAccount);
        verify(accountRepository, never()).delete(any());
    }

    // ─────────────────────────────────────────────────────
    // SECURITY
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should verify account belongs to user")
    void should_verify_account_belongs_to_user() {
        // ARRANGE
        when(accountRepository.existsByIdAndUserId(1L, USER_ID)).thenReturn(true);
        when(accountRepository.existsByIdAndUserId(1L, 99L)).thenReturn(false);

        // ACT & ASSERT
        assertThat(accountService.accountBelongsToUser(1L, USER_ID)).isTrue();
        assertThat(accountService.accountBelongsToUser(1L, 99L)).isFalse();
    }
}