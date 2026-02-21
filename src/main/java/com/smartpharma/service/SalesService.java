//package com.smartpharma.service;
//
//import com.smartpharma.entity.Pharmacy;
//import com.smartpharma.entity.SaleTransaction;
//import com.smartpharma.repository.PharmacyRepository;
//import com.smartpharma.repository.SaleTransactionRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class SalesService {
//
//    private final SaleTransactionRepository saleTransactionRepository;
//    private final PharmacyRepository pharmacyRepository;
//
//    @Transactional(readOnly = true)
//    public Page<SaleTransaction> getAllSales(Long pharmacyId, int page, int size) {
//        return saleTransactionRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size));
//    }
//
//    @Transactional(readOnly = true)
//    public SaleTransaction getSaleById(Long id, Long pharmacyId) {
//        SaleTransaction sale = saleTransactionRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("Sale not found"));
//        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
//            throw new RuntimeException("Access denied");
//        }
//        return sale;
//    }
//
//    @Transactional
//    public SaleTransaction createSale(Map<String, Object> saleRequest, Long pharmacyId) {
//        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
//                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));
//
//        SaleTransaction sale = new SaleTransaction();
//        sale.setPharmacy(pharmacy);
//        sale.setInvoiceNumber("INV-" + System.currentTimeMillis());
//
//        Object totalAmountObj = saleRequest.get("totalAmount");
//        if (totalAmountObj instanceof Number) {
//            sale.setTotalAmount(BigDecimal.valueOf(((Number) totalAmountObj).doubleValue()));
//        } else {
//            sale.setTotalAmount(BigDecimal.ZERO);
//        }
//
//        String paymentMethodStr = (String) saleRequest.getOrDefault("paymentMethod", "CASH");
//        try {
//            sale.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(paymentMethodStr.toUpperCase()));
//        } catch (IllegalArgumentException e) {
//            sale.setPaymentMethod(SaleTransaction.PaymentMethod.CASH);
//        }
//
//        sale.setCustomerPhone((String) saleRequest.get("customerPhone"));
//
//        Object discountObj = saleRequest.get("discountAmount");
//        if (discountObj instanceof Number) {
//            sale.setDiscountAmount(BigDecimal.valueOf(((Number) discountObj).doubleValue()));
//        } else {
//            sale.setDiscountAmount(BigDecimal.ZERO);
//        }
//
//        return saleTransactionRepository.save(sale);
//    }
//
//    @Transactional
//    public SaleTransaction updateSale(Long id, Map<String, Object> saleRequest, Long pharmacyId) {
//        SaleTransaction sale = getSaleById(id, pharmacyId);
//
//        if (saleRequest.get("totalAmount") instanceof Number) {
//            sale.setTotalAmount(BigDecimal.valueOf(((Number) saleRequest.get("totalAmount")).doubleValue()));
//        }
//        if (saleRequest.get("discountAmount") instanceof Number) {
//            sale.setDiscountAmount(BigDecimal.valueOf(((Number) saleRequest.get("discountAmount")).doubleValue()));
//        }
//        if (saleRequest.get("paymentMethod") instanceof String) {
//            try {
//                sale.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(((String) saleRequest.get("paymentMethod")).toUpperCase()));
//            } catch (IllegalArgumentException e) {
//            }
//        }
//        if (saleRequest.get("customerPhone") instanceof String) {
//            sale.setCustomerPhone((String) saleRequest.get("customerPhone"));
//        }
//
//        return saleTransactionRepository.save(sale);
//    }
//
//    @Transactional
//    public void deleteSale(Long id, Long pharmacyId) {
//        SaleTransaction sale = getSaleById(id, pharmacyId);
//        sale.setDeletedAt(LocalDateTime.now());
//        saleTransactionRepository.save(sale);
//    }
//
//    @Transactional(readOnly = true)
//    public Map<String, Object> getTodaySales(Long pharmacyId) {
//        LocalDate today = LocalDate.now();
//        LocalDateTime startDate = today.atStartOfDay();
//        LocalDateTime endDate = today.atTime(java.time.LocalTime.MAX);
//
//        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//        BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("totalAmount", total != null ? total : BigDecimal.ZERO);
//        response.put("count", count != null ? count : 0L);
//        return response;
//    }
//
//    @Transactional(readOnly = true)
//    public Map<String, Object> getTodaySalesSummary(Long pharmacyId) {
//        LocalDate today = LocalDate.now();
//        LocalDateTime startDate = today.atStartOfDay();
//        LocalDateTime endDate = today.atTime(java.time.LocalTime.MAX);
//
//        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//        BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("totalAmount", total != null ? total : BigDecimal.ZERO);
//        response.put("count", count != null ? count : 0L);
//
//        return response;
//    }
//
//    @Transactional(readOnly = true)
//    public Map<String, Object> getSalesStats(Long pharmacyId) {
//        Map<String, Object> stats = new HashMap<>();
//        LocalDate today = LocalDate.now();
//        LocalDateTime startDate = today.atStartOfDay();
//        LocalDateTime endDate = today.atTime(java.time.LocalTime.MAX);
//
//        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//        BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
//
//        stats.put("todaySales", total != null ? total : BigDecimal.ZERO);
//        stats.put("todayCount", count != null ? count : 0L);
//        stats.put("totalProducts", 0L);
//
//        return stats;
//    }
//
//    @Transactional(readOnly = true)
//    public Page<SaleTransaction> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
//        return saleTransactionRepository.findByPharmacyIdAndDateRange(
//                pharmacyId,
//                startDate.atStartOfDay(),
//                endDate.atTime(java.time.LocalTime.MAX),
//                PageRequest.of(0, 10)
//        );
//    }
//
//    @Transactional(readOnly = true)
//    public Page<SaleTransaction> searchSales(Long pharmacyId, String query) {
//        return saleTransactionRepository.searchSales(pharmacyId, query, PageRequest.of(0, 10));
//    }
//}