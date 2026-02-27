package com.smartpharma.repository;

import com.smartpharma.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.pharmacy.id = :pharmacyId 
        AND (n.recipient.id = :userId OR n.recipient.id IS NULL) 
        ORDER BY n.createdAt DESC
    """)
    Page<Notification> findByPharmacyIdAndRecipientId(
            @Param("pharmacyId") Long pharmacyId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
        SELECT n FROM Notification n 
        WHERE n.pharmacy.id = :pharmacyId 
        AND (n.recipient.id = :userId OR n.recipient.id IS NULL) 
        AND n.read = false 
        ORDER BY n.createdAt DESC
    """)
    List<Notification> findByPharmacyIdAndRecipientIdAndReadFalseOrderByCreatedAtDesc(
            @Param("pharmacyId") Long pharmacyId,
            @Param("userId") Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND (n.recipient.id = :userId OR n.recipient.id IS NULL) AND n.read = false")
    Long countUnreadByUser(@Param("pharmacyId") Long pharmacyId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.pharmacy.id = :pharmacyId AND (n.recipient.id = :userId OR n.recipient.id IS NULL) AND n.read = false")
    int markAllAsReadByUser(@Param("pharmacyId") Long pharmacyId, @Param("userId") Long userId);

    @Query("""
        SELECT COUNT(n) > 0 FROM Notification n
        WHERE n.relatedEntityType = :relatedEntityType
        AND n.relatedEntityId = :relatedEntityId
        AND n.type = :type
        AND n.createdAt > :createdAt
    """)
    boolean existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") Long relatedEntityId,
            @Param("type") Notification.NotificationType type,
            @Param("createdAt") LocalDateTime createdAt
    );
}