package com.fintrack.auth_service.repository;

import com.fintrack.auth_service.entity.RefreshToken;
import com.fintrack.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user")
    void revokeAllUserTokens(User user);

    @Modifying
    @Query(
        value = "DELETE FROM refresh_tokens WHERE expires_at < NOW() OR revoked = true",
        nativeQuery = true
    )
    void deleteExpiredAndRevokedTokens();
}
