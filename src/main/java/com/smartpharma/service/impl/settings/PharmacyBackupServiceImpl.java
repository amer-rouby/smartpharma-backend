package com.smartpharma.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartpharma.dto.settings.request.BackupRequest;
import com.smartpharma.dto.settings.response.BackupResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.settings.BackupRecord;
import com.smartpharma.repository.*;
import com.smartpharma.repository.settings.BackupRecordRepository;
import com.smartpharma.service.settings.PharmacyBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyBackupServiceImpl implements PharmacyBackupService {

    private static final int MAX_ROWS_PER_TABLE = 200_000;

    private final BackupRecordRepository backupRepository;
    private final PharmacyRepository pharmacyRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Override
    @Transactional
    public BackupResponse createBackup(Long pharmacyId, Long userId, BackupRequest request) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", LocalDateTime.now().toString());
        export.put("pharmacyId", pharmacyId);
        export.put("pharmacyName", pharmacy.getName());
        export.put("products", productRepository.findByPharmacyId(pharmacyId));
        export.put("categories", categoryRepository.findByPharmacyId(pharmacyId));
        export.put("suppliers", supplierRepository.findByPharmacyId(pharmacyId));
        export.put("stockBatches", stockBatchRepository.findByPharmacyId(pharmacyId));
        export.put("saleTransactions",
                saleTransactionRepository.findByPharmacyId(pharmacyId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("saleItems", saleItemRepository.findByPharmacyId(pharmacyId));
        export.put("purchaseOrders",
                purchaseOrderRepository.findByPharmacyId(pharmacyId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("purchaseOrderItems", purchaseOrderItemRepository.findByPharmacyId(pharmacyId));
        export.put("expenses", expenseRepository.findByPharmacyIdAndDeletedAtIsNull(pharmacyId));
        export.put("payments",
                paymentRepository.findByPharmacyId(pharmacyId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("users", userRepository.findByPharmacyId(pharmacyId).stream()
                // Never include password hashes in an exportable file.
                .map(u -> {
                    Map<String, Object> safe = new LinkedHashMap<>();
                    safe.put("id", u.getId());
                    safe.put("username", u.getUsername());
                    safe.put("fullName", u.getFullName());
                    safe.put("email", u.getEmail());
                    safe.put("phone", u.getPhone());
                    safe.put("role", u.getRole());
                    safe.put("isActive", u.getIsActive());
                    return safe;
                })
                .collect(Collectors.toList()));

        try {
            Path backupPath = Paths.get(backupDirectory, "pharmacy");
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "pharmacy_" + pharmacyId + "_" + sanitize(request.getBackupName()) + "_" + timestamp + ".json";
            Path fullPath = backupPath.resolve(fileName);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fullPath.toFile(), export);
            long fileSize = Files.size(fullPath);

            BackupRecord record = BackupRecord.builder()
                    .backupName(request.getBackupName())
                    .filePath(fullPath.toString())
                    .fileSize(fileSize)
                    .backupType(request.getBackupType())
                    .status("COMPLETED")
                    .description(request.getDescription())
                    .createdBy(userId)
                    .pharmacyId(pharmacyId)
                    .scope("PHARMACY")
                    .build();
            record = backupRepository.save(record);

            log.info("Pharmacy backup created: pharmacyId={}, file={}, {} bytes", pharmacyId, fileName, fileSize);
            return toResponse(record);

        } catch (IOException e) {
            log.error("Error creating pharmacy backup for pharmacyId={}", pharmacyId, e);
            throw new RuntimeException("Failed to create backup: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackupResponse> getBackupsForPharmacy(Long pharmacyId) {
        return backupRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BackupResponse getBackup(Long id, Long pharmacyId) {
        return toResponse(findOwned(id, pharmacyId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBackup(Long id, Long pharmacyId) {
        BackupRecord record = findOwned(id, pharmacyId);
        try {
            return Files.readAllBytes(Paths.get(record.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to download backup: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteBackup(Long id, Long pharmacyId) {
        BackupRecord record = findOwned(id, pharmacyId);
        try {
            Files.deleteIfExists(Paths.get(record.getFilePath()));
        } catch (IOException e) {
            log.error("Error deleting pharmacy backup file", e);
        }
        backupRepository.delete(record);
        log.info("Pharmacy backup deleted: id={}, pharmacyId={}", id, pharmacyId);
    }

    /** Loads a backup by id and verifies it actually belongs to this pharmacy and is
     * a PHARMACY-scope record - never lets a pharmacy admin reach a whole-database
     * PLATFORM backup or another pharmacy's export through this path. */
    private BackupRecord findOwned(Long id, Long pharmacyId) {
        BackupRecord record = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found"));
        if (!"PHARMACY".equals(record.getScope()) || !pharmacyId.equals(record.getPharmacyId())) {
            throw new RuntimeException("Backup not found");
        }
        return record;
    }

    private BackupResponse toResponse(BackupRecord record) {
        return BackupResponse.fromEntity(record);
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "backup";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
