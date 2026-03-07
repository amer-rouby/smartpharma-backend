package com.smartpharma.service.impl;

import com.smartpharma.dto.request.SupplierRequest;
import com.smartpharma.dto.response.SupplierResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Supplier;
import com.smartpharma.entity.User;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.SupplierRepository;
import com.smartpharma.service.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final PharmacyRepository pharmacyRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers(Long pharmacyId) {
        return supplierRepository.findByPharmacyId(pharmacyId).stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponse> getSuppliersPaginated(Long pharmacyId, int page, int size) {
        return supplierRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size))
                .map(SupplierResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Long id, Long pharmacyId) {
        Supplier supplier = supplierRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        return SupplierResponse.fromEntity(supplier);
    }

    @Override
    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request, Long pharmacyId, Long userId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        if (supplierRepository.existsByPharmacyIdAndNameAndDeletedAtIsNull(pharmacyId, request.getName())) {
            throw new RuntimeException("Supplier with this name already exists");
        }

        Supplier supplier = Supplier.builder()
                .pharmacy(pharmacy)
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .city(request.getCity())
                .status(request.getStatus())
                .notes(request.getNotes())
                .build();

        Supplier saved = supplierRepository.save(supplier);
        log.info("Supplier created: id={}, name={}, pharmacy={}", saved.getId(), saved.getName(), pharmacyId);
        return SupplierResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request, Long pharmacyId, Long userId) {
        Supplier supplier = supplierRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        supplier.setCity(request.getCity());
        supplier.setStatus(request.getStatus());
        supplier.setNotes(request.getNotes());

        Supplier updated = supplierRepository.save(supplier);
        log.info("Supplier updated: id={}, name={}", updated.getId(), updated.getName());
        return SupplierResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteSupplier(Long id, Long pharmacyId, Long userId) {
        Supplier supplier = supplierRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        supplier.setDeletedAt(java.time.LocalDateTime.now());
        supplierRepository.save(supplier);
        log.info("Supplier deleted (soft): id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countSuppliers(Long pharmacyId) {
        return supplierRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponse> searchSuppliers(Long pharmacyId, String query) {
        return supplierRepository.searchByPharmacyId(pharmacyId, query).stream()
                .map(SupplierResponse::fromEntity)
                .toList();
    }
}