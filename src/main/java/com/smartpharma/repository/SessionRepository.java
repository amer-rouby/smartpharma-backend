package com.smartpharma.repository;

import com.smartpharma.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.token = :token")
    Optional<Session> findByToken(String token);

    @Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.token = :token")
    Optional<Session> findByTokenWithUser(@Param("token") String token);

    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Session s SET s.revoked = true WHERE s.token = :token")
    void revokeByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.expiresAt < :now OR s.revoked = true")
    void cleanupExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Session s SET s.lastActivityAt = :now WHERE s.token = :token")
    void updateLastActivity(@Param("token") String token, @Param("now") LocalDateTime now);

    @Query("SELECT s FROM Session s WHERE s.user.id = :userId AND s.revoked = false AND s.expiresAt > :now ORDER BY s.expiresAt DESC")
    List<Session> findByUserIdAndRevokedFalseAndExpiresAtAfter(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
