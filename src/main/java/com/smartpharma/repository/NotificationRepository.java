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

    // ================================
    // ✅ Basic Queries
    // ================================

    Page<Notification> findByPharmacyIdAndRecipientId(Long pharmacyId, Long userId, Pageable pageable);

    // ✅ FIXED: Added OrderBy for consistent unread notifications list
    List<Notification> findByPharmacyIdAndRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Long pharmacyId, Long userId);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.recipient.id = :userId AND n.read = false")
    Long countUnreadByUser(@Param("pharmacyId") Long pharmacyId, @Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.type = :type ORDER BY n.createdAt DESC")
    List<Notification> findByType(
            @Param("pharmacyId") Long pharmacyId,
            @Param("type") Notification.NotificationType type,
            Pageable pageable);

    // ================================
    // ✅ Check for duplicate notifications (مهم لمنع التكرار)
    // ================================

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

    // ================================
    // ✅ Bulk Operations
    // ================================

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.pharmacy.id = :pharmacyId AND n.recipient.id = :userId AND n.read = false")
    int markAllAsReadByUser(@Param("pharmacyId") Long pharmacyId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.createdAt < :cutoffDate")
    void deleteOldNotifications(@Param("pharmacyId") Long pharmacyId, @Param("cutoffDate") LocalDateTime cutoffDate);

    // ================================
    // ✅ Stats & Reports
    // ================================

    @Query("SELECT n.type, COUNT(n) FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.createdAt >= :startDate GROUP BY n.type")
    List<Object[]> countByTypeAndDateRange(
            @Param("pharmacyId") Long pharmacyId,
            @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.type = :type AND n.read = false")
    Long countUnreadByType(
            @Param("pharmacyId") Long pharmacyId,
            @Param("type") Notification.NotificationType type);

    // ================================
    // ✅ Additional Helper Methods
    // ================================

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.pharmacy.id = :pharmacyId AND n.read = false")
    Long countUnreadByPharmacy(@Param("pharmacyId") Long pharmacyId);

    @Query("""
        SELECT n FROM Notification n
        WHERE n.pharmacy.id = :pharmacyId
        AND n.read = false
        ORDER BY n.priority DESC, n.createdAt DESC
    """)
    List<Notification> findUnreadByPharmacy(
            @Param("pharmacyId") Long pharmacyId,
            Pageable pageable);
}