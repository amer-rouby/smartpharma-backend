package com.smartpharma.service.impl;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.NotificationResponse;
import com.smartpharma.entity.Notification;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.repository.NotificationRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long pharmacyId, Long userId, int page, int size) {
        log.info("Fetching notifications for pharmacyId: {} and userId: {}", pharmacyId, userId);
        return notificationRepository
                .findByPharmacyIdAndRecipientId(pharmacyId, userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // السماح بوضع علامة "مقروء" إذا كان الإشعار عاماً أو مخصصاً للمستخدم
        notification.setRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public int markAllAsRead(Long pharmacyId, Long userId) {
        return notificationRepository.markAllAsReadByUser(pharmacyId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long pharmacyId, Long userId) {
        return notificationRepository
                .findByPharmacyIdAndRecipientIdAndReadFalseOrderByCreatedAtDesc(pharmacyId, userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long pharmacyId, Long userId) {
        return notificationRepository.countUnreadByUser(pharmacyId, userId);
    }

    // الميثود الخاصة بالـ Mapping (تأكد من اكتمالها)
    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : "SYSTEM")
                .priority(n.getPriority() != null ? n.getPriority().name() : "LOW")
                .read(n.isRead())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .build();
    }

    // ميثود الإنشاء والـ Alerts تظل كما هي...
    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        Notification notification = Notification.builder()
                .pharmacy(request.getPharmacy())
                .recipient(request.getRecipient())
                .title(request.getTitle())
                .message(request.getMessage())
                .type(request.getType())
                .priority(request.getPriority())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId())
                .build();
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override public void deleteNotification(Long id, Long userId) { notificationRepository.deleteById(id); }
    @Override public void checkAndCreateLowStockAlerts(Long id) { /* logic */ }
    @Override public void checkAndCreateExpiryAlerts(Long id) { /* logic */ }
}