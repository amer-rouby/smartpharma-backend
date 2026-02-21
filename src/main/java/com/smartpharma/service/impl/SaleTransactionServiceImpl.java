package com.smartpharma.service.impl;

import com.smartpharma.dto.request.SaleItemRequest;
import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.SaleTransactionDTO;
import com.smartpharma.entity.*;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.SaleTransactionRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.SaleTransactionService;
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
public class SaleTransactionServiceImpl implements SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size) {
        log.debug("Fetching sales for pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);
        return saleTransactionRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size))
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleTransactionDTO getSaleById(Long id, Long pharmacyId) {
        log.debug("Fetching sale {} for pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }
        return mapToDTO(sale);
    }

    @Override
    @Transactional
    public SaleTransactionDTO createSale(SaleRequest request, Long currentUserId) {
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

            // الحصول على Active Batch للمنتج
            StockBatch batch = null;
            List<StockBatch> batches = stockBatchRepository.findByProductIdAndStatusActive(product.getId());
            if (batches != null && !batches.isEmpty()) {
                batch = batches.get(0);

                if (batch.getQuantityCurrent() != null &&
                        batch.getQuantityCurrent() < itemRequest.getQuantity()) {
                    throw new RuntimeException("Insufficient stock for product: " + product.getName());
                }
            }

            SaleItem saleItem = SaleItem.builder()
                    .product(product)
                    .batch(batch)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .build();
            saleItem.calculateTotalPrice();
            saleItem.setTransaction(sale);
            saleItems.add(saleItem);

            // خصم المخزون
            if (batch != null && batch.getQuantityCurrent() != null) {
                batch.setQuantityCurrent(batch.getQuantityCurrent() - itemRequest.getQuantity());
                stockBatchRepository.save(batch);
            }
        }

        sale.setItems(saleItems);
        sale.calculateTotals();

        SaleTransaction savedSale = saleTransactionRepository.save(sale);
        log.info("Sale created with ID: {}, total: {}", savedSale.getId(), savedSale.getTotalAmount());

        return mapToDTO(savedSale);
    }

    @Override
    @Transactional
    public SaleTransactionDTO updateSale(Long id, SaleRequest request, Long pharmacyId) {
        log.info("Updating sale {} for pharmacyId: {}", id, pharmacyId);

        // Re-fetch entity for update (not using DTO)
        SaleTransaction entity = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!entity.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        if (request.getDiscountAmount() != null) {
            entity.setDiscountAmount(request.getDiscountAmount());
        }
        if (request.getPaymentMethod() != null) {
            entity.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase()));
        }
        if (request.getCustomerPhone() != null) {
            entity.setCustomerPhone(request.getCustomerPhone());
        }

        entity.calculateTotals();
        SaleTransaction updated = saleTransactionRepository.save(entity);

        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteSale(Long id, Long pharmacyId) {
        log.info("Soft deleting sale {} for pharmacyId: {}", id, pharmacyId);

        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found"));

        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        sale.setDeletedAt(LocalDateTime.now());
        saleTransactionRepository.save(sale);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySalesSummary(Long pharmacyId) {
        return getTodaySales(pharmacyId);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        return saleTransactionRepository.findByPharmacyIdAndDateRange(
                pharmacyId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                PageRequest.of(0, 10)
        ).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> searchSales(Long pharmacyId, String query) {
        return saleTransactionRepository.searchSales(pharmacyId, query, PageRequest.of(0, 10))
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleTransactionDTO> getRecentSales(Long pharmacyId, int limit) {
        return saleTransactionRepository.findTop10ByPharmacyIdOrderByTransactionDateDesc(pharmacyId)
                .stream()
                .limit(limit)
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 🔧 Helper Method: Entity → DTO Mapping
    // ==========================================

    private SaleTransactionDTO mapToDTO(SaleTransaction sale) {
        return SaleTransactionDTO.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .subtotal(sale.getSubtotal())
                .totalAmount(sale.getTotalAmount())
                .discountAmount(sale.getDiscountAmount())
                .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null)
                .customerPhone(sale.getCustomerPhone())
                .transactionDate(sale.getTransactionDate())
                .pharmacyId(sale.getPharmacy().getId())
                .pharmacyName(sale.getPharmacy().getName())
                .items(sale.getItems() != null ? sale.getItems().stream()
                        .map(this::mapItemToDTO)
                        .collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    private SaleTransactionDTO.SaleItemDTO mapItemToDTO(SaleItem item) {
        return SaleTransactionDTO.SaleItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .barcode(item.getProduct().getBarcode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}