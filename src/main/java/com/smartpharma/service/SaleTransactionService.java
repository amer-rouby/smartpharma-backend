package com.smartpharma.service;

import com.smartpharma.dto.request.SaleItemRequest;
import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.SaleTransactionResponse;
import com.smartpharma.entity.*;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StockBatchRepository stockBatchRepository;

    @Transactional(readOnly = true)
    public Page<SaleTransaction> getAllSales(Long pharmacyId, int page, int size) {
        log.debug("Fetching sales for pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);
        return saleTransactionRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public SaleTransaction getSaleById(Long id, Long pharmacyId) {
        log.debug("Fetching sale {} for pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }
        return sale;
    }

    @Transactional
    public SaleTransactionResponse createSale(SaleRequest request, Long currentUserId) {
        log.info("Creating sale for pharmacyId: {}, items count: {}", request.getPharmacyId(), request.getItems().size());

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        SaleTransaction sale = SaleTransaction.builder()
                .pharmacy(pharmacy)
                .invoiceNumber("INV-" + System.currentTimeMillis())
                .discountAmount(request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO)
                .customerPhone(request.getCustomerPhone())
                .paymentMethod(SaleTransaction.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()))
                .build();

        List<SaleItem> saleItems = new ArrayList<>();

        for (SaleItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

            BigDecimal availableStock = getAvailableStock(product.getId());
            if (availableStock.compareTo(BigDecimal.valueOf(itemRequest.getQuantity())) < 0) {
                throw new RuntimeException("Insufficient stock for product: " + product.getName());
            }

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            saleItem.calculateTotalPrice();
            saleItem.setTransaction(sale);
            saleItems.add(saleItem);

            deductStock(product.getId(), itemRequest.getQuantity());
        }

        sale.setItems(saleItems);
        sale.calculateTotals();

        SaleTransaction savedSale = saleTransactionRepository.save(sale);
        log.info("Sale created with ID: {}, total: {}", savedSale.getId(), savedSale.getTotalAmount());

        return mapToResponse(savedSale);
    }

    @Transactional
    public SaleTransactionResponse updateSale(Long id, SaleRequest request, Long pharmacyId) {
        log.info("Updating sale {} for pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = getSaleById(id, pharmacyId);

        if (request.getDiscountAmount() != null) {
            sale.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getPaymentMethod() != null) {
            sale.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }
        if (request.getCustomerPhone() != null) {
            sale.setCustomerPhone(request.getCustomerPhone());
        }

        sale.calculateTotals();
        SaleTransaction updated = saleTransactionRepository.save(sale);

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteSale(Long id, Long pharmacyId) {
        log.info("Soft deleting sale {} for pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = getSaleById(id, pharmacyId);
        sale.setDeletedAt(LocalDateTime.now());
        saleTransactionRepository.save(sale);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySales(Long pharmacyId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);

        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
        BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total != null ? total : BigDecimal.ZERO);
        response.put("count", count != null ? count : 0L);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySalesSummary(Long pharmacyId) {
        return getTodaySales(pharmacyId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStats(Long pharmacyId) {
        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);

        Long count = saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
        BigDecimal total = saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);

        stats.put("todaySales", total != null ? total : BigDecimal.ZERO);
        stats.put("todayCount", count != null ? count : 0L);
        stats.put("totalProducts", productRepository.countByPharmacyId(pharmacyId));

        return stats;
    }

    @Transactional(readOnly = true)
    public Page<SaleTransaction> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        return saleTransactionRepository.findByPharmacyIdAndDateRange(
                pharmacyId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                PageRequest.of(0, 10)
        );
    }

    @Transactional(readOnly = true)
    public Page<SaleTransaction> searchSales(Long pharmacyId, String query) {
        return saleTransactionRepository.searchSales(pharmacyId, query, PageRequest.of(0, 10));
    }

    @Transactional(readOnly = true)
    public List<SaleTransactionResponse> getRecentSales(Long pharmacyId, int limit) {
        return saleTransactionRepository.findTop10ByPharmacyIdOrderByTransactionDateDesc(pharmacyId)
                .stream()
                .limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private BigDecimal getAvailableStock(Long productId) {
        Long sum = stockBatchRepository.sumQuantityByProductId(productId);
        return sum != null ? BigDecimal.valueOf(sum) : BigDecimal.ZERO;
    }

    private void deductStock(Long productId, Integer quantity) {
        List<StockBatch> batches = stockBatchRepository.findByProductIdAndStatusActive(productId);
        int remaining = quantity;

        for (StockBatch batch : batches) {
            if (remaining <= 0) break;

            Integer available = batch.getQuantityCurrent();
            if (available == null || available <= 0) continue;

            int toDeduct = Math.min(available, remaining);
            batch.setQuantityCurrent(available - toDeduct);
            stockBatchRepository.save(batch);
            remaining -= toDeduct;
        }

        if (remaining > 0) {
            log.warn("Could not fully deduct stock for product {}: remaining {}", productId, remaining);
        }
    }

    private SaleTransactionResponse mapToResponse(SaleTransaction sale) {
        return SaleTransactionResponse.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .subtotal(sale.getSubtotal())
                .totalAmount(sale.getTotalAmount())
                .discountAmount(sale.getDiscountAmount())
                .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null)
                .customerPhone(sale.getCustomerPhone())
                .transactionDate(sale.getTransactionDate())
                .items(sale.getItems() != null ? sale.getItems().stream()
                        .map(item -> SaleTransactionResponse.SaleItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct().getId())
                                .productName(item.getProduct().getName())
                                .barcode(item.getProduct().getBarcode())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build())
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }
}