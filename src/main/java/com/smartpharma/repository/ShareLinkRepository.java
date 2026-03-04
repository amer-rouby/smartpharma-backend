package com.smartpharma.repository;

import com.smartpharma.entity.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {

    Optional<ShareLink> findByToken(String token);

    @Query("""
        SELECT sl FROM ShareLink sl
        WHERE sl.token = :token
        AND sl.isActive = true
        AND sl.expiresAt > :currentTime
    """)
    Optional<ShareLink> findActiveByToken(
            @Param("token") String token,
            @Param("currentTime") LocalDateTime currentTime);

    @Modifying
    @Query("""
        UPDATE ShareLink sl
        SET sl.accessCount = COALESCE(sl.accessCount, 0) + 1
        WHERE sl.token = :token
    """)
    void incrementAccessCount(@Param("token") String token);

    @Modifying
    @Query("""
        UPDATE ShareLink sl
        SET sl.isActive = false
        WHERE sl.expiresAt < :currentTime
    """)
    void deactivateExpiredLinks(@Param("currentTime") LocalDateTime currentTime);
}