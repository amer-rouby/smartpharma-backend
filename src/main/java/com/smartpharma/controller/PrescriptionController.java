package com.smartpharma.controller;

import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Prescription images are sensitive medical documents, so unlike profile pictures
 * (owner-only) these are scoped per-pharmacy: stored under a pharmacyId subfolder and
 * only ever read/written using SecurityUtils.getCurrentPharmacyId() (derived server-side
 * from the authenticated user), never a client-supplied pharmacyId - the same pattern
 * SalesController already uses everywhere to prevent cross-tenant access.
 */
@RestController
@RequestMapping("/api/sales/prescriptions")
@Slf4j
public class PrescriptionController {

    @Value("${app.prescription-upload-dir:uploads/prescriptions/}")
    private String uploadDir;

    private static final List<String> ALLOWED_TYPES =
            List.of("image/png", "image/jpeg", "image/jpg", "image/webp");
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPrescription(
            @RequestParam("file") MultipartFile file) {

        Long pharmacyId = SecurityUtils.getCurrentPharmacyId();

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("No file selected"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file type. Allowed: PNG, JPEG, WEBP"));
            }
            if (file.getSize() > MAX_SIZE_BYTES) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File size must be less than 5MB"));
            }

            String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
            String extension = originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String filename = UUID.randomUUID() + extension;

            Path pharmacyUploadPath = Paths.get(uploadDir, pharmacyId.toString()).normalize();
            if (!Files.exists(pharmacyUploadPath)) {
                Files.createDirectories(pharmacyUploadPath);
            }

            Path filePath = pharmacyUploadPath.resolve(filename).normalize();
            if (!filePath.startsWith(pharmacyUploadPath)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid filename"));
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            String publicUrl = "/api/sales/prescriptions/" + pharmacyId + "/" + filename;

            log.info("Prescription image uploaded | pharmacyId: {} | filename: {}", pharmacyId, filename);
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("url", publicUrl, "filename", filename), "Prescription uploaded successfully"));

        } catch (IOException e) {
            log.error("Error uploading prescription image for pharmacyId: {}", pharmacyId, e);
            return ResponseEntity.status(500).body(ApiResponse.error("Upload failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{pharmacyId}/{filename}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> getPrescriptionImage(
            @PathVariable Long pharmacyId,
            @PathVariable String filename) {

        Long currentPharmacyId = SecurityUtils.getCurrentPharmacyId();
        if (!currentPharmacyId.equals(pharmacyId)) {
            log.warn("Denied cross-tenant prescription access | requested pharmacyId: {} | actual pharmacyId: {}",
                    pharmacyId, currentPharmacyId);
            throw new AccessDeniedException("Access denied");
        }

        try {
            String safeFilename = Paths.get(filename).getFileName().toString();
            Path filePath = Paths.get(uploadDir, pharmacyId.toString(), safeFilename).normalize();
            Resource resource = new FileSystemResource(filePath);
            if (resource.exists()) {
                return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
