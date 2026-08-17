package com.smartpharma.service.impl;

import com.smartpharma.dto.request.NotificationRequest;
import com.smartpharma.dto.response.NotificationResponse;
import com.smartpharma.entity.Notification;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.entity.User;
import com.smartpharma.entity.settings.BackupRecord;
import com.smartpharma.entity.settings.NotificationSettings;
import com.smartpharma.repository.*;
import com.smartpharma.repository.settings.BackupRecordRepository;
import com.smartpharma.repository.settings.NotificationSettingsRepository;
import com.smartpharma.repository.settings.PharmacySettingsRepository;
import com.smartpharma.service.EmailService;
import com.smartpharma.service.NotificationService;
import com.smartpharma.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
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
    private final NotificationStreamService notificationStreamService;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final PharmacySettingsRepository pharmacySettingsRepository;
    private final BackupRecordRepository backupRecordRepository;
    private final EmailService emailService;

    // Self-injected lazy proxy so dispatchEmailForNotification (called from saveNotificationDirect,
    // both in this class) goes through the Spring AOP proxy instead of a same-class call, which
    // is required for @Async to actually take effect - a direct `this.` call bypasses the proxy
    // entirely and would run synchronously despite the annotation. Field injection (not
    // constructor, which is what @RequiredArgsConstructor would generate for a final field)
    // because constructor-time self-injection throws BeanCurrentlyInCreationException even
    // with @Lazy - the proxy still needs to resolve the bean name during argument creation.
    @Autowired
    @Lazy
    private NotificationServiceImpl self;

    private static final int EXPIRY_WARNING_DAYS = 30;
    private static final int BACKUP_REMINDER_STALE_DAYS = 7;

    @Override @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(Long pharmacyId, Long userId, int page, int size) {
        NotificationSettings settings = resolveSettings(userId);
        Page<Notification> pageResult = notificationRepository.findByPharmacyIdAndRecipientId(
                pharmacyId, userId, PageRequest.of(page, size));
        // Filtered in-memory rather than in the query itself, since "which types this
        // user wants to see" is per-user preference data, not something worth pushing
        // into a dynamic JPQL predicate. totalElements stays pre-filter, so a page can
        // come back with fewer rows than requested when a user has muted some types -
        // an acceptable trade-off for a low-volume internal notification list.
        List<NotificationResponse> filtered = pageResult.getContent().stream()
                .filter(n -> isVisibleToUser(n, settings))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return new org.springframework.data.domain.PageImpl<>(filtered, pageResult.getPageable(), pageResult.getTotalElements());
    }

    @Override @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId, Long pharmacyId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Notification not found");
        }
        if (notification.getRecipient() == null || notification.getRecipient().getId().equals(userId)) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            NotificationResponse response = mapToResponse(notificationRepository.save(notification));
            notificationStreamService.notifyChanged(notification.getPharmacy().getId());
            return response;
        }
        throw new RuntimeException("Unauthorized to mark this notification as read");
    }

    @Override @Transactional
    public int markAllAsRead(Long pharmacyId, Long userId) {
        int count = notificationRepository.markAllAsReadByUser(pharmacyId, userId);
        notificationStreamService.notifyChanged(pharmacyId);
        return count;
    }

    @Override @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(Long pharmacyId, Long userId) {
        NotificationSettings settings = resolveSettings(userId);
        return notificationRepository.findByPharmacyIdAndRecipientIdAndReadFalseOrderByCreatedAtDesc(pharmacyId, userId)
                .stream()
                .filter(n -> isVisibleToUser(n, settings))
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override @Transactional(readOnly = true)
    public Long getUnreadCount(Long pharmacyId, Long userId) {
        return (long) getUnreadNotifications(pharmacyId, userId).size();
    }

    @Override @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        // Prevent duplicate notifications within 24h. Keyed only by (entityType, entityId,
        // type) - fine for the single-row broadcast notifications this guards (recipient
        // == null, one row serves the whole pharmacy), but NOT safe for fanning the same
        // event out to several individual recipients - use saveNotificationDirect for that.
        if (request.getRelatedEntityType() != null && request.getRelatedEntityId() != null) {
            if (notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                    request.getRelatedEntityType(), request.getRelatedEntityId(), request.getType(),
                    LocalDateTime.now().minusHours(24))) {
                return null;
            }
        }
        return saveNotificationDirect(request);
    }

    /** Saves + broadcasts + emails a notification with no dedup check - the caller is
     * responsible for not calling this redundantly. Needed when the same event fans out
     * to multiple individual recipients (e.g. a lockout notifying the user AND every
     * admin), since createNotification's dedup key doesn't consider the recipient and
     * would silently drop every row after the first for the same related entity. */
    private NotificationResponse saveNotificationDirect(NotificationRequest request) {
        Notification notification = Notification.builder()
                .pharmacy(request.getPharmacy()).recipient(request.getRecipient())
                .title(request.getTitle()).message(request.getMessage())
                .titleEn(request.getTitleEn()).messageEn(request.getMessageEn())
                .type(request.getType()).priority(request.getPriority())
                .relatedEntityType(request.getRelatedEntityType())
                .relatedEntityId(request.getRelatedEntityId()).build();
        NotificationResponse response = mapToResponse(notificationRepository.save(notification));
        notificationStreamService.notifyCreated(request.getPharmacy().getId(), response);
        self.dispatchEmailForNotification(notification);
        return response;
    }

    /**
     * Emails every eligible recipient for a just-created notification, respecting
     * each user's own emailNotifications/notify* toggles and quiet hours. A broadcast
     * notification (recipient == null) fans out to every active user in the pharmacy
     * individually, since "who wants an email" is a per-user choice, not a pharmacy-wide
     * one. Never allowed to fail the notification itself - SMTP problems are logged and
     * swallowed, exactly like the rest of this method's best-effort alert checks.
     *
     * @Async: runs on a separate thread so a slow/misconfigured SMTP server (multi-second
     * connection/read timeouts, potentially once per recipient) never blocks the request
     * that triggered the notification - e.g. a POS sale used to wait on this synchronously.
     */
    @Async
    public void dispatchEmailForNotification(Notification notification) {
        List<User> recipients = notification.getRecipient() != null
                ? List.of(notification.getRecipient())
                : userRepository.findActiveByPharmacyId(notification.getPharmacy().getId());

        for (User user : recipients) {
            try {
                if (user.getEmail() == null || user.getEmail().isBlank()) {
                    continue;
                }
                NotificationSettings settings = resolveSettings(user.getId());
                if (!Boolean.TRUE.equals(settings.getEmailNotifications())) {
                    continue;
                }
                if (!isTypeEnabledForUser(settings, notification.getType())) {
                    continue;
                }
                if (isWithinQuietHours(settings)) {
                    continue;
                }
                boolean english = "en".equalsIgnoreCase(settings.getPreferredLanguage());
                String subject = english && notification.getTitleEn() != null ? notification.getTitleEn() : notification.getTitle();
                String body = english && notification.getMessageEn() != null ? notification.getMessageEn() : notification.getMessage();
                emailService.sendPlainTextEmail(user.getEmail(), subject, body);
            } catch (Exception e) {
                log.warn("Failed to email notification {} to userId {}: {}", notification.getId(), user.getId(), e.getMessage());
            }
        }
    }

    /** Loads a user's saved NotificationSettings, or the same defaults
     * NotificationSettingsServiceImpl would create for them on first visit - without
     * writing a row, since this runs from background checks and shouldn't have the
     * side effect of silently creating settings rows for users who never opened the
     * settings screen. */
    private NotificationSettings resolveSettings(Long userId) {
        return notificationSettingsRepository.findByUserId(userId)
                .orElseGet(() -> NotificationSettings.builder()
                        .emailNotifications(true).smsNotifications(false).pushNotifications(true)
                        .preferredLanguage("ar")
                        .soundEnabled(true).vibrationEnabled(true)
                        .quietHoursEnabled(false).quietHoursStart("22:00").quietHoursEnd("08:00")
                        .notifyLowStock(true).notifyOutOfStock(true)
                        .notifyExpiryWarning(true).notifyExpiredProducts(true)
                        .notifyNewSale(false).notifyLargeSale(true).notifyRefund(true)
                        .notifyNewExpense(true).notifyLargeExpense(true)
                        .notifySystemUpdates(true).notifyBackupReminder(true).notifySecurityAlerts(true)
                        .build());
    }

    private boolean isVisibleToUser(Notification notification, NotificationSettings settings) {
        if (!Boolean.TRUE.equals(settings.getPushNotifications())) {
            return false;
        }
        return isTypeEnabledForUser(settings, notification.getType());
    }

    private boolean isTypeEnabledForUser(NotificationSettings settings, Notification.NotificationType type) {
        return switch (type) {
            case LOW_STOCK -> Boolean.TRUE.equals(settings.getNotifyLowStock());
            case OUT_OF_STOCK -> Boolean.TRUE.equals(settings.getNotifyOutOfStock());
            case EXPIRY_WARNING -> Boolean.TRUE.equals(settings.getNotifyExpiryWarning());
            case EXPIRED -> Boolean.TRUE.equals(settings.getNotifyExpiredProducts());
            case SALE_COMPLETED -> Boolean.TRUE.equals(settings.getNotifyNewSale());
            case LARGE_SALE -> Boolean.TRUE.equals(settings.getNotifyLargeSale());
            case EXPENSE_ADDED -> Boolean.TRUE.equals(settings.getNotifyNewExpense());
            case LARGE_EXPENSE -> Boolean.TRUE.equals(settings.getNotifyLargeExpense());
            case BACKUP_REMINDER -> Boolean.TRUE.equals(settings.getNotifyBackupReminder());
            case SECURITY_ALERT -> Boolean.TRUE.equals(settings.getNotifySecurityAlerts());
            case SYSTEM -> true;
        };
    }

    /** quietHoursStart/End are "HH:mm" strings and may wrap past midnight (e.g.
     * 22:00-08:00), so this can't just compare start &lt;= now &lt;= end. */
    private boolean isWithinQuietHours(NotificationSettings settings) {
        if (!Boolean.TRUE.equals(settings.getQuietHoursEnabled())) {
            return false;
        }
        try {
            LocalTime start = LocalTime.parse(settings.getQuietHoursStart());
            LocalTime end = LocalTime.parse(settings.getQuietHoursEnd());
            LocalTime now = LocalTime.now();
            if (start.equals(end)) {
                return false;
            }
            if (start.isBefore(end)) {
                return !now.isBefore(start) && now.isBefore(end);
            }
            return !now.isBefore(start) || now.isBefore(end);
        } catch (DateTimeParseException | NullPointerException e) {
            return false;
        }
    }

    @Override @Transactional
    public void deleteNotification(Long id, Long userId, Long pharmacyId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        if (!notification.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Notification not found");
        }
        if (notification.getRecipient() == null || notification.getRecipient().getId().equals(userId)) {
            notificationRepository.delete(notification);
            notificationStreamService.notifyChanged(notification.getPharmacy().getId());
        } else {
            throw new RuntimeException("Unauthorized to delete this notification");
        }
    }

    @Override @Transactional
    public void checkAndCreateLowStockAlerts(Long pharmacyId) {
        log.info("Checking low stock alerts for pharmacy: {}", pharmacyId);
        for (Product product : productRepository.findLowStockProducts(pharmacyId)) {
            Long currentStock = stockBatchRepository.sumQuantityByProductId(product.getId());
            // Zero stock and "running low" are different urgency levels with their own
            // toggle (notifyOutOfStock vs notifyLowStock) - findLowStockProducts already
            // covers both (stock <= minStockLevel includes stock == 0), split them here.
            Notification.NotificationType type = (currentStock == null || currentStock <= 0)
                    ? Notification.NotificationType.OUT_OF_STOCK
                    : Notification.NotificationType.LOW_STOCK;
            if (!notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                    "PRODUCT", product.getId(), type, LocalDateTime.now().minusHours(24))) {
                createLowStockNotification(product, pharmacyId, currentStock, type, null);
            }
        }
    }

    private void createLowStockNotification(Product product, Long pharmacyId, Long currentStock,
                                              Notification.NotificationType type, User recipient) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        long stock = currentStock == null ? 0 : currentStock;
        boolean outOfStock = type == Notification.NotificationType.OUT_OF_STOCK;
        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(recipient)
                .title(outOfStock ? "❌ نفاد المخزون" : "⚠️ تنبيه مخزون منخفض")
                .message(outOfStock
                        ? "نفد مخزون المنتج '" + product.getName() + "' تماماً"
                        : "وصل مخزون المنتج '" + product.getName() + "' إلى مستوى منخفض: " + stock + " وحدة")
                .titleEn(outOfStock ? "❌ Out of Stock" : "⚠️ Low Stock Warning")
                .messageEn(outOfStock
                        ? "Product '" + product.getName() + "' is out of stock"
                        : "Product '" + product.getName() + "' has reached low stock: " + stock + " units")
                .type(type)
                .priority(outOfStock ? Notification.NotificationPriority.URGENT : Notification.NotificationPriority.HIGH)
                .relatedEntityType("PRODUCT").relatedEntityId(product.getId()).build());
    }

    @Override @Transactional
    public void checkAndCreateExpiryAlerts(Long pharmacyId) {
        log.info("Checking expiry alerts for pharmacy: {}", pharmacyId);
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(EXPIRY_WARNING_DAYS);

        // Expired batches
        for (StockBatch batch : stockBatchRepository.findExpiredBatches(pharmacyId, today)) {
            createExpiryNotification(batch, pharmacyId, "EXPIRED",
                    Notification.NotificationType.EXPIRED, Notification.NotificationPriority.URGENT);
        }
        // Expiring soon batches
        for (StockBatch batch : stockBatchRepository.findExpiringBatches(pharmacyId, warningDate)) {
            if (!batch.getExpiryDate().isBefore(today) &&
                    !notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                            "STOCK_BATCH", batch.getId(), Notification.NotificationType.EXPIRY_WARNING,
                            LocalDateTime.now().minusHours(24))) {
                createExpiryNotification(batch, pharmacyId, "EXPIRING_SOON",
                        Notification.NotificationType.EXPIRY_WARNING, Notification.NotificationPriority.MEDIUM);
            }
        }
    }

    private void createExpiryNotification(StockBatch batch, Long pharmacyId, String statusLabel,
                                          Notification.NotificationType type, Notification.NotificationPriority priority) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        boolean expired = type == Notification.NotificationType.EXPIRED;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), batch.getExpiryDate());

        String daysTextAr = days > 0 ? "خلال " + days + " يوم" : "اليوم";
        String statusLabelAr = expired ? "منتهية الصلاحية" : "قريبة من انتهاء الصلاحية";
        String titleAr = expired ? "❌ منتج منتهي الصلاحية" : "⚠️ صلاحية قريبة";
        String messageAr = "دفعة '" + batch.getBatchNumber() + "' من المنتج '" + batch.getProduct().getName() + "' " + statusLabelAr + " (" + daysTextAr + ")";

        String daysTextEn = days > 0 ? "in " + days + " day(s)" : "today";
        String titleEn = expired ? "❌ Expired Product" : "⚠️ Expiring Soon";
        String messageEn = "Batch '" + batch.getBatchNumber() + "' of product '" + batch.getProduct().getName() + "' " + statusLabel + " (" + daysTextEn + ")";

        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(null)
                .title(titleAr).message(messageAr)
                .titleEn(titleEn).messageEn(messageEn)
                .type(type).priority(priority)
                .relatedEntityType("STOCK_BATCH").relatedEntityId(batch.getId()).build());
    }

    @Override @Transactional
    public void notifySaleCompleted(Long pharmacyId, Long saleId, BigDecimal totalAmount) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        var pharmacySettings = pharmacySettingsRepository.findByPharmacyId(pharmacyId);
        BigDecimal threshold = pharmacySettings.map(s -> s.getLargeSaleThreshold()).orElse(BigDecimal.valueOf(5000));
        String currency = pharmacySettings.map(s -> s.getCurrency()).orElse("EGP");
        boolean large = totalAmount != null && threshold != null && totalAmount.compareTo(threshold) >= 0;

        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(null)
                .title(large ? "💰 عملية بيع كبيرة" : "🛒 عملية بيع جديدة")
                .message((large ? "تمت عملية بيع كبيرة بقيمة: " : "تمت عملية بيع بقيمة: ") + totalAmount + " " + currency)
                .titleEn(large ? "💰 Large Sale" : "🛒 New Sale")
                .messageEn((large ? "A large sale was completed: " : "A sale was completed: ") + totalAmount + " " + currency)
                .type(large ? Notification.NotificationType.LARGE_SALE : Notification.NotificationType.SALE_COMPLETED)
                .priority(large ? Notification.NotificationPriority.HIGH : Notification.NotificationPriority.LOW)
                .relatedEntityType("SALE").relatedEntityId(saleId).build());
    }

    @Override @Transactional
    public void notifyExpenseAdded(Long pharmacyId, Long expenseId, BigDecimal amount) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
        var pharmacySettings = pharmacySettingsRepository.findByPharmacyId(pharmacyId);
        BigDecimal threshold = pharmacySettings.map(s -> s.getLargeExpenseThreshold()).orElse(BigDecimal.valueOf(2000));
        String currency = pharmacySettings.map(s -> s.getCurrency()).orElse("EGP");
        boolean large = amount != null && threshold != null && amount.compareTo(threshold) >= 0;

        createNotification(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(null)
                .title(large ? "💸 مصروف كبير" : "🧾 مصروف جديد")
                .message((large ? "تم تسجيل مصروف كبير بقيمة: " : "تم تسجيل مصروف بقيمة: ") + amount + " " + currency)
                .titleEn(large ? "💸 Large Expense" : "🧾 New Expense")
                .messageEn((large ? "A large expense was recorded: " : "An expense was recorded: ") + amount + " " + currency)
                .type(large ? Notification.NotificationType.LARGE_EXPENSE : Notification.NotificationType.EXPENSE_ADDED)
                .priority(large ? Notification.NotificationPriority.HIGH : Notification.NotificationPriority.LOW)
                .relatedEntityType("EXPENSE").relatedEntityId(expenseId).build());
    }

    @Override @Transactional
    public void notifySecurityAlert(Long lockedOutUserId) {
        User lockedOutUser = userRepository.findById(lockedOutUserId).orElse(null);
        if (lockedOutUser == null || lockedOutUser.getPharmacy() == null) {
            return;
        }
        Pharmacy pharmacy = lockedOutUser.getPharmacy();
        String messageAr = "تم قفل الحساب '" + lockedOutUser.getUsername() + "' بعد عدة محاولات دخول فاشلة";
        String messageEn = "Account '" + lockedOutUser.getUsername() + "' was locked after too many failed login attempts";

        // The affected user should know their own account got locked, and admins should
        // know so they can investigate/unlock it - each as its own recipient-targeted
        // row (saveNotificationDirect, not createNotification: they'd all share the same
        // relatedEntityId and trip each other's dedup check otherwise).
        saveNotificationDirect(NotificationRequest.builder()
                .pharmacy(pharmacy).recipient(lockedOutUser)
                .title("🔒 تم قفل الحساب").message(messageAr)
                .titleEn("🔒 Account Locked").messageEn(messageEn)
                .type(Notification.NotificationType.SECURITY_ALERT).priority(Notification.NotificationPriority.URGENT)
                .relatedEntityType("USER").relatedEntityId(lockedOutUser.getId()).build());

        for (User admin : userRepository.findActiveByPharmacyId(pharmacy.getId())) {
            if (admin.getRole() == User.UserRole.ADMIN && !admin.getId().equals(lockedOutUser.getId())) {
                saveNotificationDirect(NotificationRequest.builder()
                        .pharmacy(pharmacy).recipient(admin)
                        .title("🔒 تم قفل الحساب").message(messageAr)
                        .titleEn("🔒 Account Locked").messageEn(messageEn)
                        .type(Notification.NotificationType.SECURITY_ALERT).priority(Notification.NotificationPriority.URGENT)
                        .relatedEntityType("USER").relatedEntityId(lockedOutUser.getId()).build());
            }
        }
    }

    @Override @Transactional
    public void checkAndCreateBackupReminders(Long pharmacyId) {
        List<BackupRecord> recent = backupRecordRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId);
        LocalDateTime lastCompleted = recent.stream()
                .filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus()))
                .map(BackupRecord::getCreatedAt)
                .findFirst().orElse(null);

        boolean stale = lastCompleted == null || lastCompleted.isBefore(LocalDateTime.now().minusDays(BACKUP_REMINDER_STALE_DAYS));
        if (!stale) {
            return;
        }
        // Dedupe key is the pharmacy itself (there's no per-backup entity id to key
        // against for "no backup happened"), so relatedEntityId is the pharmacyId.
        if (notificationRepository.existsByRelatedEntityTypeAndRelatedEntityIdAndTypeAndCreatedAtAfter(
                "PHARMACY", pharmacyId, Notification.NotificationType.BACKUP_REMINDER,
                LocalDateTime.now().minusDays(BACKUP_REMINDER_STALE_DAYS))) {
            return;
        }

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId).orElse(null);
        if (pharmacy == null) {
            return;
        }
        String messageAr = lastCompleted == null
                ? "لم يتم عمل نسخة احتياطية لهذه الصيدلية من قبل - يُنصح بعمل نسخة احتياطية"
                : "مر أكثر من " + BACKUP_REMINDER_STALE_DAYS + " أيام منذ آخر نسخة احتياطية - يُنصح بعمل نسخة جديدة";
        String messageEn = lastCompleted == null
                ? "This pharmacy has never been backed up - consider running a backup"
                : "The last backup was over " + BACKUP_REMINDER_STALE_DAYS + " days ago - consider running a new one";

        for (User admin : userRepository.findActiveByPharmacyId(pharmacyId)) {
            if (admin.getRole() == User.UserRole.ADMIN) {
                saveNotificationDirect(NotificationRequest.builder()
                        .pharmacy(pharmacy).recipient(admin)
                        .title("🗄️ تذكير بالنسخ الاحتياطي").message(messageAr)
                        .titleEn("🗄️ Backup Reminder").messageEn(messageEn)
                        .type(Notification.NotificationType.BACKUP_REMINDER).priority(Notification.NotificationPriority.MEDIUM)
                        .relatedEntityType("PHARMACY").relatedEntityId(pharmacyId).build());
            }
        }
    }

    @Scheduled(cron = "0 0 * * * *") @Transactional
    public void runScheduledAlerts() {
        log.info("Running scheduled notification checks...");
        for (Pharmacy pharmacy : pharmacyRepository.findByDeletedAtIsNull()) {
            try {
                checkAndCreateLowStockAlerts(pharmacy.getId());
                checkAndCreateExpiryAlerts(pharmacy.getId());
                checkAndCreateBackupReminders(pharmacy.getId());
            } catch (Exception e) {
                log.error("Error running alerts for pharmacy {}: {}", pharmacy.getId(), e.getMessage());
            }
        }
    }

    private NotificationResponse mapToResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage())
                .titleEn(n.getTitleEn()).messageEn(n.getMessageEn())
                .type(n.getType() != null ? n.getType().name() : "SYSTEM")
                .priority(n.getPriority() != null ? n.getPriority().name() : "LOW")
                .read(n.isRead())
                .createdAt(n.getCreatedAt() != null ? n.getCreatedAt().toString() : null)
                .relatedEntityType(n.getRelatedEntityType())
                .relatedEntityId(n.getRelatedEntityId()).build();
    }
}
