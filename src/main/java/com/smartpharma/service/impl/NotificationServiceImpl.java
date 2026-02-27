package com.smartpharma.service.impl;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.NotificationResponse;
import com.smartpharma.entity.Notification;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.repository.*;
import com.smartpharma.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;

    private static final int EXPIRY_WARNING_DAYS = 30;

    @Override @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long pharmacyId, Long userId, int page, int size) {
        return notificationRepository.findByPharmacyIdAndRecipientId(pharmacyId, userId, PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    @Override @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (notification.getRecipient() == null || notification.getRecipient().getId().equals(userId)) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            return mapToResponse(notificationRepository.save(notification));
        }
        throw new RuntimeException("Unauthorized to mark this notification as read");
    }

    @Override @Transactional
    public int markAllAsRead(Long pharmacyId, Long userId) {
        return notificationRepository.markAllAsReadByUser(pharmacyId, userId);
    }

    @Override @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long pharmacyId, Long userId) {
        return notificationRepository.findByPharmacyIdAndRecipientIdAndReadFalseOrderByCreatedAtDesc(pharmacyId, userId)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public Long getUnreadCount(Long pharmacyId, Long userId) {
        return notificationRepository.countUnreadByUser(pharmacyId, userId);
    }

    @Override @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // Prevent duplicate notifications within 24h
        if (request.getRelatedEntityType() != null && request.getRelatedEntityId() != null) {
            if (notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                    request.getRelatedEntityType(), request.getRelatedEntityId(), request.getType(),
                    LocalDateTime.now().minusHours(24))) {
                return null;
            }
        }
        Notification notification = Notification.builder()
                .pharmacy(request.getPharmacy()).recipient(request.getRecipient())
                .title(request.getTitle()).message(request.getMessage())
                .type(request.getType()).priority(request.getPriority())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId()).build();
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override @Transactional
    public void deleteNotification(Long id, Long userId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (notification.getRecipient() == null || notification.getRecipient().getId().equals(userId)) {
            notificationRepository.delete(notification);
        } else {
            throw new RuntimeException("Unauthorized to delete this notification");
        }
    }

    @Override @Transactional
    public void checkAndCreateLowStockAlerts(Long pharmacyId) {
        log.info("Checking low stock alerts for pharmacy: {}", pharmacyId);
        for (Product product : productRepository.findLowStockProducts(pharmacyId)) {
            if (!notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                    "PRODUCT", product.getId(), Notification.NotificationType.LOW_STOCK,
                    LocalDateTime.now().minusHours(24))) {
                createLowStockNotification(product, pharmacyId, null);
            }
        }
    }

    private void createLowStockNotification(Product product, Long pharmacyId, User recipient) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        Long currentStock = stockBatchRepository.sumQuantityByProductId(product.getId());
        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(recipient)
                .title("⚠️ مخزون منخفض")
                .message("المنتج '" + product.getName() + "' وصل إلى مخزون منخفض: " + currentStock + " وحدة")
                .type(Notification.NotificationType.LOW_STOCK)
                .priority(Notification.NotificationPriority.HIGH)
                .relatedEntityType("PRODUCT").relatedEntityId(product.getId()).build());
    }

    @Override @Transactional
    public void checkAndCreateExpiryAlerts(Long pharmacyId) {
        log.info("Checking expiry alerts for pharmacy: {}", pharmacyId);
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(EXPIRY_WARNING_DAYS);

        // Expired batches
        for (StockBatch batch : stockBatchRepository.findExpiredBatches(pharmacyId, today)) {
            createExpiryNotification(batch, pharmacyId, "منتهي الصلاحية",
                    Notification.NotificationType.EXPIRED, Notification.NotificationPriority.URGENT);
        }
        // Expiring soon batches
        for (StockBatch batch : stockBatchRepository.findExpiringBatches(pharmacyId, warningDate)) {
            if (!batch.getExpiryDate().isBefore(today) &&
                    !notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                            "STOCK_BATCH", batch.getId(), Notification.NotificationType.EXPIRY_WARNING,
                            LocalDateTime.now().minusHours(24))) {
                createExpiryNotification(batch, pharmacyId, "ينتهي قريباً",
                        Notification.NotificationType.EXPIRY_WARNING, Notification.NotificationPriority.MEDIUM);
            }
        }
    }

    private void createExpiryNotification(StockBatch batch, Long pharmacyId, String statusLabel,
                                          Notification.NotificationType type, Notification.NotificationPriority priority) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        long days = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());
        String daysText = days > 0 ? "خلال " + days + " يوم" : "اليوم";
        String title = type == Notification.NotificationType.EXPIRED ? "❌ منتهي الصلاحية" : "⚠️ ينتهي قريباً";
        String message = "دفعة '" + batch.getBatchNumber() + "' من المنتج '" + batch.getProduct().getName() + "' " + statusLabel + " (" + daysText + ")";

        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(null)
                .title(title).message(message).type(type).priority(priority)
                .relatedEntityType("STOCK_BATCH").relatedEntityId(batch.getId()).build());
    }

    @Scheduled(cron = "0 0 * * * *") @Transactional
    public void runScheduledAlerts() {
        log.info("Running scheduled notification checks...");
        for (Pharmacy pharmacy : pharmacyRepository.findByDeletedAtIsNull()) {
            try {
                checkAndCreateLowStockAlerts(pharmacy.getId());
                checkAndCreateExpiryAlerts(pharmacy.getId());
            } catch (Exception e) {
                log.error("Error running alerts for pharmacy {}: {}", pharmacy.getId(), e.getMessage());
            }
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .type(n.getType() != null ? n.getType().name() : "SYSTEM")
                .priority(n.getPriority() != null ? n.getPriority().name() : "LOW")
                .read(n.isRead())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId()).build();
    }
}