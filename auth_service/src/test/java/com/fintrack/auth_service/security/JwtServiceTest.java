package com.fintrack.auth_service.security;

import com.fintrack.auth_service.entity.Role;
import com.fintrack.auth_service.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        // Inyectamos los valores de @Value manualmente
        // porque no arrancamos Spring en tests unitarios
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "fintrack-super-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900000L);

        testUser = User.builder()
                .id(1L)
                .email("jose@gmail.com")
                .firstName("Jose")
                .lastName("Garcia")
                .password("hashedpassword")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("Should generate a valid JWT token for a user")
    void should_generate_token_successfully() {
        // ACT
        String token = jwtService.generateToken(testUser);

        // ASSERT
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        // Un JWT tiene 3 partes separadas por puntos
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("Should extract email from token correctly")
    void should_extract_email_from_token() {
        // ARRANGE
        String token = jwtService.generateToken(testUser);

        // ACT
        String extractedEmail = jwtService.extractEmail(token);

        // ASSERT
        assertThat(extractedEmail).isEqualTo("jose@gmail.com");
    }

    @Test
    @DisplayName("Should validate token successfully for correct user")
    void should_validate_token_for_correct_user() {
        // ARRANGE
        String token = jwtService.generateToken(testUser);

        // ACT
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // ASSERT
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject token for different user")
    void should_reject_token_for_different_user() {
        // ARRANGE
        String token = jwtService.generateToken(testUser);

        User differentUser = User.builder()
                .id(2L)
                .email("otro@gmail.com")
                .firstName("Otro")
                .lastName("Usuario")
                .password("hashedpassword")
                .role(Role.ROLE_USER)
                .enabled(true)
                .build();

        // ACT
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // ASSERT
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should detect expired token")
    void should_detect_expired_token() {
        // ARRANGE — creamos un JwtService con expiración de -1ms (ya expirado)
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "secretKey",
            "fintrack-super-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm");
        ReflectionTestUtils.setField(expiredJwtService, "jwtExpiration", -1000L);

        String expiredToken = expiredJwtService.generateToken(testUser);

        // ACT & ASSERT
        // JJWT lanza ExpiredJwtException cuando el token está expirado
        // así que verificamos que isTokenValid devuelve false (que captura la excepción internamente)
        boolean isValid = jwtService.isTokenValid(expiredToken, testUser);
            assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should return false for valid (non-expired) token")
    void should_return_false_for_valid_token() {
        // ARRANGE
        String token = jwtService.generateToken(testUser);

        // ACT
        boolean isExpired = jwtService.isTokenExpired(token);

        // ASSERT
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Should extract expiration date from token")
    void should_extract_expiration_date() {
        // ARRANGE
        String token = jwtService.generateToken(testUser);

        // ACT
        Date expiration = jwtService.extractExpiration(token);

        // ASSERT
        assertThat(expiration).isNotNull();
        assertThat(expiration).isAfter(new Date());
    }
}