package com.smartpharma.service;

import com.smartpharma.dto.request.SupplierRequest;
import com.smartpharma.dto.response.SupplierResponse;
import org.springframework.data.domain.Page;
import java.util.List;

public interface SupplierService {
    List<SupplierResponse> getAllSuppliers(Long pharmacyId);
    Page<SupplierResponse> getSuppliersPaginated(Long pharmacyId, int page, int size);
    SupplierResponse getSupplier(Long id, Long pharmacyId);
    SupplierResponse createSupplier(SupplierRequest request, Long pharmacyId, Long userId);
    SupplierResponse updateSupplier(Long id, SupplierRequest request, Long pharmacyId, Long userId);
    void deleteSupplier(Long id, Long pharmacyId, Long userId);
    Long countSuppliers(Long pharmacyId);
    List<SupplierResponse> searchSuppliers(Long pharmacyId, String query);
}