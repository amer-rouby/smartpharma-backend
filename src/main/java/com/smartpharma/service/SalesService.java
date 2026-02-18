package com.smartpharma.service;

import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.SaleTransaction;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final PharmacyRepository pharmacyRepository;

    @Transactional(readOnly = true)
    public Page<SaleTransaction> getAllSales(Long pharmacyId, int page, int size) {
        return saleTransactionRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public SaleTransaction getSaleById(Long id, Long pharmacyId) {
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        return sale;
    }

    @Transactional
    public SaleTransaction createSale(Map<String, Object> saleRequest, Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        SaleTransaction sale = new SaleTransaction();
        sale.setPharmacy(pharmacy);
        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());

        Object totalAmountObj = saleRequest.get("totalAmount");
        if (totalAmountObj instanceof Number) {
            sale.setTotalAmount(java.math.BigDecimal.valueOf(((Number) totalAmountObj).doubleValue()));
        } else {
            sale.setTotalAmount(java.math.BigDecimal.ZERO);
        }

        String paymentMethodStr = (String) saleRequest.getOrDefault("paymentMethod", "CASH");
        try {
            sale.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(paymentMethodStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            sale.setPaymentMethod(SaleTransaction.PaymentMethod.CASH);
        }

        String customerPhone = (String) saleRequest.get("customerPhone");
        sale.setCustomerPhone(customerPhone);

        Object discountObj = saleRequest.get("discountAmount");
        if (discountObj instanceof Number) {
            sale.setDiscountAmount(java.math.BigDecimal.valueOf(((Number) discountObj).doubleValue()));
        } else {
            sale.setDiscountAmount(java.math.BigDecimal.ZERO);
        }

        return saleTransactionRepository.save(sale);
    }

    @Transactional
    public void deleteSale(Long id, Long pharmacyId) {
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        sale.setDeletedAt(java.time.LocalDateTime.now());
        saleTransactionRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySales(Long pharmacyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(java.time.LocalTime.MAX);

        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
        java.math.BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total != null ? total : 0);
        response.put("count", count != null ? count : 0);

        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStats(Long pharmacyId) {
        Map<String, Object> stats = new HashMap<>();

        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(java.time.LocalTime.MAX);

        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
        java.math.BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);

        stats.put("todaySales", total != null ? total : 0);
        stats.put("todayCount", count != null ? count : 0);
        stats.put("totalProducts", 0);

        return stats;
    }

    @Transactional(readOnly = true)
    public Page<SaleTransaction> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        return saleTransactionRepository.findByPharmacyIdAndDateRange(
                pharmacyId,
                startDate.atStartOfDay(),
                endDate.atTime(java.time.LocalTime.MAX),
                PageRequest.of(0, 10)
        );
    }

    @Transactional(readOnly = true)
    public Page<SaleTransaction> searchSales(Long pharmacyId, String query) {
        return saleTransactionRepository.searchSales(pharmacyId, query, PageRequest.of(0, 10));
    }
}
