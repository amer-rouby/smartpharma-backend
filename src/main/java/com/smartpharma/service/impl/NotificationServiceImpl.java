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

        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getRecipient() != null && !notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        notification.setRead(true);
        notification.setReadAt(java.time.LocalDateTime.now());

        return mapToResponse(notificationRepository.save(notification));
    }

    // ✅ FIXED: Added missing method implementation
    @Override
    @Transactional
    public int markAllAsRead(Long pharmacyId, Long userId) {
        return notificationRepository.markAllAsReadByUser(pharmacyId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long pharmacyId, Long userId, int page, int size) {
        return notificationRepository
                .findByPharmacyIdAndRecipientId(pharmacyId, userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
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

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (notification.getRecipient() != null && !notification.getRecipient().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        notificationRepository.delete(notification);
    }

    @Override
    @Transactional
    public void checkAndCreateLowStockAlerts(Long pharmacyId) {
        List<Product> lowStockProducts = productRepository.findLowStockProducts(pharmacyId);

        for (Product product : lowStockProducts) {
            boolean alreadyNotified = notificationRepository
                    .existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                            "PRODUCT", product.getId(),
                            Notification.NotificationType.LOW_STOCK,
                            LocalDate.now().atStartOfDay());

            if (!alreadyNotified) {
                createNotification(NotificationRequest.builder()
                        .pharmacy(product.getPharmacy())
                        .title("تنبيه: مخزون منخفض")
                        .message("المنتج '" + product.getName() + "' وصل للمخزون الأدنى (" + product.getTotalStock() + ")")
                        .type(Notification.NotificationType.LOW_STOCK)
                        .priority(Notification.NotificationPriority.HIGH)
                        .relatedEntityType("PRODUCT")
                        .relatedEntityId(product.getId())
                        .build());
            }
        }
    }

    @Override
    @Transactional
    public void checkAndCreateExpiryAlerts(Long pharmacyId) {
        LocalDate warningDate = LocalDate.now().plusDays(30);
        LocalDate expiredDate = LocalDate.now();

        List<StockBatch> expiringSoon = stockBatchRepository.findExpiringBatches(pharmacyId, warningDate);
        for (StockBatch batch : expiringSoon) {
            boolean alreadyNotified = notificationRepository
                    .existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                            "STOCK_BATCH", batch.getId(),
                            Notification.NotificationType.EXPIRY_WARNING,
                            LocalDate.now().atStartOfDay());

            if (!alreadyNotified) {
                createNotification(NotificationRequest.builder()
                        .pharmacy(batch.getPharmacy())
                        .title("تنبيه: صلاحية قريبة من الانتهاء")
                        .message("دفعة '" + batch.getBatchNumber() + "' من المنتج '" + batch.getProduct().getName() +
                                "' ستنتهي صلاحيتها في " + batch.getExpiryDate())
                        .type(Notification.NotificationType.EXPIRY_WARNING)
                        .priority(Notification.NotificationPriority.MEDIUM)
                        .relatedEntityType("STOCK_BATCH")
                        .relatedEntityId(batch.getId())
                        .build());
            }
        }

        List<StockBatch> expired = stockBatchRepository.findByPharmacyIdAndStatus(pharmacyId, StockBatch.BatchStatus.ACTIVE)
                .stream()
                .filter(sb -> sb.getExpiryDate() != null && sb.getExpiryDate().isBefore(expiredDate))
                .toList();

        for (StockBatch batch : expired) {
            boolean alreadyNotified = notificationRepository
                    .existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                            "STOCK_BATCH", batch.getId(),
                            Notification.NotificationType.EXPIRED,
                            LocalDate.now().atStartOfDay());

            if (!alreadyNotified) {
                createNotification(NotificationRequest.builder()
                        .pharmacy(batch.getPharmacy())
                        .title("⚠️ منتهية الصلاحية")
                        .message("دفعة '" + batch.getBatchNumber() + "' من المنتج '" + batch.getProduct().getName() +
                                "' انتهت صلاحيتها!")
                        .type(Notification.NotificationType.EXPIRED)
                        .priority(Notification.NotificationPriority.URGENT)
                        .relatedEntityType("STOCK_BATCH")
                        .relatedEntityId(batch.getId())
                        .build());
            }
        }
    }

    // ✅ Helper: Map Entity to Response
    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name())
                .priority(n.getPriority().name())
                .read(n.isRead())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId())
                .build();
    }
}