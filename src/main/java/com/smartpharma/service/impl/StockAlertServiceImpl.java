package com.smartpharma.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpharma.dto.response.AlertStatsResponse;
import com.smartpharma.dto.response.StockAlertResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockAlert;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockAlertRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertRepository alertRepository;
    private final StockBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponse> getAlerts(Long pharmacyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return alertRepository.findByPharmacyId(pharmacyId, pageable)
                .map(StockAlertResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponse> getAlertsByStatus(Long pharmacyId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return alertRepository.findByPharmacyIdAndStatus(pharmacyId, status, pageable)
                .map(StockAlertResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAlertResponse> getActiveAlerts(Long pharmacyId) {
        return alertRepository.findActiveAlerts(pharmacyId)
                .stream()
                .map(StockAlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStatsResponse getAlertStats(Long pharmacyId) {
        Long totalAlerts = alertRepository.countByPharmacyId(pharmacyId);
        Long unreadAlerts = alertRepository.countUnreadAlerts(pharmacyId);
        Long lowStockAlerts = alertRepository.countActiveAlertsByType(pharmacyId, StockAlert.AlertType.LOW_STOCK);
        Long expiredAlerts = alertRepository.countActiveAlertsByType(pharmacyId, StockAlert.AlertType.EXPIRED);
        Long expiringSoonAlerts = alertRepository.countActiveAlertsByType(pharmacyId, StockAlert.AlertType.EXPIRING_SOON);
        Long outOfStockAlerts = alertRepository.countActiveAlertsByType(pharmacyId, StockAlert.AlertType.OUT_OF_STOCK);

        return AlertStatsResponse.builder()
                .totalAlerts(totalAlerts)
                .unreadAlerts(unreadAlerts)
                .lowStockAlerts(lowStockAlerts)
                .expiredAlerts(expiredAlerts)
                .expiringSoonAlerts(expiringSoonAlerts)
                .outOfStockAlerts(outOfStockAlerts)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Long alertId, Long pharmacyId, Long userId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        alert.setStatus("READ");
        alert.setReadAt(LocalDateTime.now());
        alertRepository.save(alert);

        log.info("Alert {} marked as read by user {}", alertId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long pharmacyId, Long userId) {
        List<StockAlert> alerts = alertRepository.findByPharmacyIdAndStatusAndCreatedAtAfter(
                pharmacyId, "UNREAD", LocalDateTime.now().minusDays(30)
        );

        alerts.forEach(alert -> {
            alert.setStatus("READ");
            alert.setReadAt(LocalDateTime.now());
        });

        alertRepository.saveAll(alerts);
        log.info("All alerts marked as read for pharmacy {} by user {}", pharmacyId, userId);
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, Long pharmacyId, Long userId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(User.builder().id(userId).build());
        alertRepository.save(alert);

        log.info("Alert {} resolved by user {}", alertId, userId);
    }

    @Override
    @Transactional
    public void deleteAlert(Long alertId, Long pharmacyId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        if (!alert.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        alertRepository.delete(alert);
        log.info("Alert {} deleted for pharmacy {}", alertId, pharmacyId);
    }

    @Override
    @Transactional
    public StockAlertResponse createAlert(Long pharmacyId,
                                          Long productId,
                                          Long batchId,
                                          StockAlert.AlertType type,
                                          String title,
                                          String message,
                                          String severity) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Product product = productId != null ? productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found")) : null;

        StockBatch batch = batchId != null ? batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found")) : null;

        // Check if similar alert already exists (avoid duplicates)
        boolean exists = alertRepository.findByPharmacyIdAndStatus(pharmacyId, "UNREAD", PageRequest.of(0, 100))
                .stream()
                .anyMatch(a -> a.getAlertType() == type &&
                        (productId != null ? a.getProduct() != null && a.getProduct().getId().equals(productId) : true) &&
                        a.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24)));

        if (exists) {
            log.info("Similar alert already exists for type: {}, product: {}", type, productId);
            return null;
        }

        StockAlert alert = StockAlert.builder()
                .pharmacy(pharmacy)
                .product(product)
                .batch(batch)
                .alertType(type)
                .title(title)
                .message(message)
                .severity(severity != null ? severity : "MEDIUM")
                .status("UNREAD")
                .build();

        // Add metadata
        Map<String, Object> metadata = new HashMap<>();
        if (product != null) {
            metadata.put("productName", product.getName());
            metadata.put("productCode", product.getCode());
        }
        if (batch != null) {
            metadata.put("batchNumber", batch.getBatchNumber());
            metadata.put("currentStock", batch.getQuantityCurrent());
            if (batch.getExpiryDate() != null) {
                metadata.put("expiryDate", batch.getExpiryDate().toString());
            }
        }

        try {
            alert.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Error serializing metadata", e);
        }

        StockAlert saved = alertRepository.save(alert);
        log.info("Alert created: type={}, product={}, message={}", type, productId, title);

        return StockAlertResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void generateLowStockAlerts(Long pharmacyId) {
        List<Product> products = productRepository.findByPharmacyId(pharmacyId);

        for (Product product : products) {
            Long totalStock = batchRepository.sumQuantityByProductId(product.getId());

            if (totalStock <= product.getMinStockLevel()) {
                String title = totalStock == 0 ? "نفاد المخزون" : "مخزون منخفض";
                String message = String.format("المنتج '%s' - المخزون الحالي: %d (الحد الأدنى: %d)",
                        product.getName(), totalStock, product.getMinStockLevel());

                StockAlert.AlertType type = totalStock == 0 ?
                        StockAlert.AlertType.OUT_OF_STOCK : StockAlert.AlertType.LOW_STOCK;

                createAlert(pharmacyId, product.getId(), null, type, title, message,
                        totalStock == 0 ? "CRITICAL" : "HIGH");
            }
        }
    }

    @Override
    @Transactional
    public void generateExpiryAlerts(Long pharmacyId) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);

        // Expired batches
        List<StockBatch> expiredBatches = batchRepository.findExpiredBatches(pharmacyId, today);
        for (StockBatch batch : expiredBatches) {
            String title = "منتج منتهي الصلاحية";
            String message = String.format("المنتج '%s' (دفعة %s) منتهي الصلاحية منذ %d يوم",
                    batch.getProduct().getName(),
                    batch.getBatchNumber(),
                    java.time.temporal.ChronoUnit.DAYS.between(batch.getExpiryDate(), today));

            createAlert(pharmacyId, batch.getProduct().getId(), batch.getId(),
                    StockAlert.AlertType.EXPIRED, title, message, "CRITICAL");
        }

        // Expiring soon batches
        List<StockBatch> expiringBatches = batchRepository.findExpiringBatches(pharmacyId, thirtyDaysLater);
        for (StockBatch batch : expiringBatches) {
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiryDate());

            String title = "ينتهي قريباً";
            String message = String.format("المنتج '%s' (دفعة %s) ينتهي خلال %d يوم",
                    batch.getProduct().getName(),
                    batch.getBatchNumber(),
                    daysUntilExpiry);

            String severity = daysUntilExpiry <= 7 ? "HIGH" : "MEDIUM";

            createAlert(pharmacyId, batch.getProduct().getId(), batch.getId(),
                    StockAlert.AlertType.EXPIRING_SOON, title, message, severity);
        }
    }
}