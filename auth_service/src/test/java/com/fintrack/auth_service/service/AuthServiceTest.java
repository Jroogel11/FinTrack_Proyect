package com.fintrack.auth_service.service;

import com.fintrack.auth_service.dto.AuthDtos.*;
import com.fintrack.auth_service.entity.RefreshToken;
import com.fintrack.auth_service.entity.Role;
import com.fintrack.auth_service.entity.User;
import com.fintrack.auth_service.exception.EmailAlreadyExistsException;
import com.fintrack.auth_service.exception.InvalidTokenException;
import com.fintrack.auth_service.repository.RefreshTokenRepository;
import com.fintrack.auth_service.repository.UserRepository;
import com.fintrack.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpiration", 900000L);
        ReflectionTestUtils.setField(authService, "refreshExpiration", 604800000L);

        testUser = User.builder()
                .id(1L)
                .email("jose@gmail.com")
                .firstName("Jose")
                .lastName("Garcia")
                .password("$2a$10$hashedpassword")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("Jose");
        registerRequest.setLastName("Garcia");
        registerRequest.setEmail("jose@gmail.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("jose@gmail.com");
        loginRequest.setPassword("password123");
    }

    // ─────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should register user successfully")
    void should_register_user_successfully() {
        // ARRANGE
        when(userRepository.existsByEmail("jose@gmail.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ACT
        AuthResponse response = authService.register(registerRequest);

        // ASSERT
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("jose@gmail.com");
        assertThat(response.getUser().getRole()).isEqualTo("ROLE_USER");

        // Verificamos que se llamó a save una vez
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void should_throw_exception_when_email_already_exists() {
        // ARRANGE
        when(userRepository.existsByEmail("jose@gmail.com")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("jose@gmail.com");

        // Verificamos que NUNCA se llamó a save (el registro se cortó antes)
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should normalize email to lowercase on register")
    void should_normalize_email_to_lowercase() {
        // ARRANGE
        registerRequest.setEmail("JOSE@GMAIL.COM");
        when(userRepository.existsByEmail("JOSE@GMAIL.COM")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashedpassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            // Verificamos que el email se normalizó
            assertThat(saved.getEmail()).isEqualTo("jose@gmail.com");
            return testUser;
        });
        when(jwtService.generateToken(any())).thenReturn("token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        authService.register(registerRequest);

        // ASSERT — verificado dentro del thenAnswer
        verify(userRepository).save(any(User.class));
    }

    // ─────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should login successfully with correct credentials")
    void should_login_successfully() {
        // ARRANGE
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(
                        testUser, null, testUser.getAuthorities()));
        when(userRepository.findByEmail("jose@gmail.com"))
                .thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        AuthResponse response = authService.login(loginRequest);

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("jose@gmail.com");

        // Verificamos que se revocaron los tokens anteriores
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(testUser);
    }

    @Test
    @DisplayName("Should throw BadCredentialsException with wrong password")
    void should_throw_exception_with_wrong_password() {
        // ARRANGE
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class);

        // Verificamos que nunca se buscó el usuario ni se generó token
        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
    }

    // ─────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should refresh token successfully")
    void should_refresh_token_successfully() {
        // ARRANGE
        RefreshToken validRefreshToken = RefreshToken.builder()
                .id(1L)
                .token("valid-refresh-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        when(refreshTokenRepository.findByToken("valid-refresh-token"))
                .thenReturn(Optional.of(validRefreshToken));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("new-jwt-token");

        // ACT
        AuthResponse response = authService.refreshToken(request);

        // ASSERT
        assertThat(response.getAccessToken()).isEqualTo("new-jwt-token");
        // Verificamos que el refresh token anterior se revocó
        assertThat(validRefreshToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void should_throw_exception_when_refresh_token_not_found() {
        // ARRANGE
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(refreshTokenRepository.findByToken("invalid-token"))
                .thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("Should throw exception when refresh token is expired")
    void should_throw_exception_when_refresh_token_expired() {
        // ARRANGE
        RefreshToken expiredToken = RefreshToken.builder()
                .id(1L)
                .token("expired-token")
                .user(testUser)
                .expiresAt(LocalDateTime.now().minusDays(1)) // expirado ayer
                .revoked(false)
                .build();

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-token");

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expiredToken));

        // ACT & ASSERT
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired or revoked");
    }

    // ─────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Should logout successfully")
    void should_logout_successfully() {
        // ARRANGE
        when(userRepository.findByEmail("jose@gmail.com"))
                .thenReturn(Optional.of(testUser));

        // ACT
        authService.logout("jose@gmail.com");

        // ASSERT
        verify(refreshTokenRepository, times(1)).revokeAllUserTokens(testUser);
    }
}