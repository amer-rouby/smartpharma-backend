package com.smartpharma.service.impl;

import com.smartpharma.dto.request.SaleItemRequest;
import com.smartpharma.dto.request.SaleRequest;
import com.smartpharma.dto.response.SaleTransactionDTO;
import com.smartpharma.dto.response.SalesReportResponse;
import com.smartpharma.entity.*;
import com.smartpharma.repository.*;
import com.smartpharma.service.SaleTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleTransactionServiceImpl implements SaleTransactionService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SaleItemRepository saleItemRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size) {
        return getAllSales(pharmacyId, page, size, "transactionDate", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getAllSales(Long pharmacyId, int page, int size,
                                                String sortBy, String sortDirection) {
        log.debug("Fetching sales | pharmacyId: {} | page: {} | size: {} | sortBy: {} | sortDirection: {}",
                pharmacyId, page, size, sortBy, sortDirection);
        Pageable pageable = createPageable(sortBy, sortDirection, page, size);
        return saleTransactionRepository.findByPharmacyId(pharmacyId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public SaleTransactionDTO getSaleById(Long id, Long pharmacyId) {
        log.debug("Fetching sale | id: {} | pharmacyId: {}", id, pharmacyId);
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found with id: " + id));
        validatePharmacyAccess(sale, pharmacyId);
        return mapToDTO(sale);
    }

    @Override
    @Transactional
    public SaleTransactionDTO createSale(SaleRequest request, Long currentUserId) {
        log.info("Creating new sale | pharmacyId: {} | items: {} | userId: {}",
                request.getPharmacyId(), request.getItems().size(), currentUserId);

        Pharmacy pharmacy = pharmacyRepository.findById(request.getPharmacyId())
                .orElseThrow(() -> new RuntimeException("Pharmacy not found: " + request.getPharmacyId()));

        SaleTransaction sale = SaleTransaction.builder()
                .pharmacy(pharmacy)
                .user(null)
                .invoiceNumber(generateInvoiceNumber())
                .discountAmount(Optional.ofNullable(request.getDiscountAmount()).orElse(BigDecimal.ZERO))
                .customerPhone(request.getCustomerPhone())
                .paymentMethod(SaleTransaction.PaymentMethod.valueOf(
                        Optional.ofNullable(request.getPaymentMethod()).orElse("CASH").toUpperCase()))
                .notes(request.getNotes())
                .build();

        List<SaleItem> saleItems = new ArrayList<>();
        for (SaleItemRequest itemRequest : request.getItems()) {
            SaleItem saleItem = processSaleItem(itemRequest, sale);
            saleItems.add(saleItem);
        }

        sale.setItems(saleItems);
        sale.calculateTotals();

        SaleTransaction savedSale = saleTransactionRepository.save(sale);
        log.info("Sale created successfully | id: {} | total: {} | items: {}",
                savedSale.getId(), savedSale.getTotalAmount(), savedSale.getItems().size());
        return mapToDTO(savedSale);
    }

    @Override
    @Transactional
    public SaleTransactionDTO updateSale(Long id, SaleRequest request, Long pharmacyId) {
        log.info("Updating sale | id: {} | pharmacyId: {}", id, pharmacyId);
        SaleTransaction entity = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + id));
        validatePharmacyAccess(entity, pharmacyId);

        Optional.ofNullable(request.getDiscountAmount()).ifPresent(entity::setDiscountAmount);
        Optional.ofNullable(request.getPaymentMethod())
                .ifPresent(pm -> entity.setPaymentMethod(SaleTransaction.PaymentMethod.valueOf(pm.toUpperCase())));
        Optional.ofNullable(request.getCustomerPhone()).ifPresent(entity::setCustomerPhone);
        Optional.ofNullable(request.getNotes()).ifPresent(entity::setNotes);

        entity.calculateTotals();
        SaleTransaction updated = saleTransactionRepository.save(entity);
        log.info("Sale updated successfully | id: {} | total: {}", updated.getId(), updated.getTotalAmount());
        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deleteSale(Long id, Long pharmacyId) {
        log.info("Soft deleting sale | id: {} | pharmacyId: {}", id, pharmacyId);
        SaleTransaction sale = saleTransactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sale not found: " + id));
        validatePharmacyAccess(sale, pharmacyId);
        sale.markAsDeleted();
        saleTransactionRepository.save(sale);
        log.info("Sale deleted successfully | id: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesReportResponse getSalesAnalytics(Long pharmacyId, LocalDate startDate,
                                                 LocalDate endDate, String period) {
        log.info("Generating sales analytics | pharmacyId: {} | period: {} to {}",
                pharmacyId, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        BigDecimal totalRevenue = Optional.ofNullable(
                saleTransactionRepository.getTotalRevenue(pharmacyId, startDateTime, endDateTime)
        ).orElse(BigDecimal.ZERO);

        Long totalOrders = Optional.ofNullable(
                saleTransactionRepository.getTotalOrders(pharmacyId, startDateTime, endDateTime)
        ).orElse(0L);

        BigDecimal averageOrder = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<SalesReportResponse.TopProductDTO> topProducts = fetchTopProducts(
                pharmacyId, startDateTime, endDateTime, 10);

        Map<String, BigDecimal> revenueByPaymentMethod = fetchRevenueByPaymentMethod(
                pharmacyId, startDateTime, endDateTime);

        Map<String, Long> ordersByDay = fetchDailyOrders(pharmacyId, startDateTime, endDateTime);

        Long totalItems = saleItemRepository.sumQuantityByPharmacyIdAndDateRange(
                pharmacyId, startDateTime, endDateTime);

        return SalesReportResponse.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .averageOrder(averageOrder)
                .totalItems(Optional.ofNullable(totalItems).orElse(0L))
                .revenueByPaymentMethod(revenueByPaymentMethod)
                .ordersByDay(ordersByDay)
                .topProducts(topProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesStats(Long pharmacyId) {
        log.debug("Fetching sales stats for pharmacyId: {}", pharmacyId);
        LocalDate today = LocalDate.now();
        LocalDateTime startDate = today.atStartOfDay();
        LocalDateTime endDate = today.atTime(LocalTime.MAX);

        Long todayCount = Optional.ofNullable(
                saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate)
        ).orElse(0L);

        BigDecimal todayTotal = Optional.ofNullable(
                saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        Long totalProducts = productRepository.countByPharmacyId(pharmacyId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("todaySales", todayTotal);
        stats.put("todayCount", todayCount);
        stats.put("totalProducts", totalProducts);
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySales(Long pharmacyId) {
        log.debug("Fetching today sales for pharmacyId: {}", pharmacyId);
        return getSalesForDate(pharmacyId, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySalesSummary(Long pharmacyId) {
        return getTodaySales(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> getSalesByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching sales by date range | pharmacyId: {} | {} to {}",
                pharmacyId, startDate, endDate);
        return saleTransactionRepository.findByPharmacyIdAndDateRange(
                pharmacyId,
                startDate.atStartOfDay(),
                endDate.atTime(LocalTime.MAX),
                PageRequest.of(0, 20)
        ).map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SaleTransactionDTO> searchSales(Long pharmacyId, String query) {
        log.info("Searching sales | pharmacyId: {} | query: {}", pharmacyId, query);
        if (query == null || query.trim().isEmpty()) {
            return Page.empty();
        }
        return saleTransactionRepository.searchSales(pharmacyId, query.trim(), PageRequest.of(0, 20))
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleTransactionDTO> getRecentSales(Long pharmacyId, int limit) {
        log.debug("Fetching recent sales | pharmacyId: {} | limit: {}", pharmacyId, limit);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return saleTransactionRepository.findRecentSalesByPharmacyId(pharmacyId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getSalesByCategory(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching sales by category | pharmacyId: {} | {} to {}",
                pharmacyId, startDate, endDate);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<SaleItem> saleItems = saleItemRepository.findByPharmacyIdAndDateRange(
                pharmacyId, startDateTime, endDateTime);
        Map<String, BigDecimal> salesByCategory = saleItems.stream()
                .filter(item -> {
                    try {
                        Product product = item.getProduct();
                        if (product == null) return false;
                        if (!Hibernate.isInitialized(product)) {
                            Hibernate.initialize(product);
                        }
                        if (product.getDeletedAt() != null) return false;
                        return product.getCategory() != null;
                    } catch (Exception e) {
                        log.warn("Skipping sale item {} due to product error: {}",
                                item.getId(), e.getMessage());
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(
                        item -> {
                            try {
                                return item.getProduct().getCategory();
                            } catch (Exception e) {
                                return "أخرى";
                            }
                        },
                        Collectors.reducing(BigDecimal.ZERO, SaleItem::getTotalPrice, BigDecimal::add)
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("salesByCategory", salesByCategory);
        response.put("totalCategories", salesByCategory.size());
        response.put("dateRange", Map.of("start", startDate, "end", endDate));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts(Long pharmacyId, int limit) {
        log.info("Fetching top products | pharmacyId: {} | limit: {}", pharmacyId, limit);
        int safeLimit = Math.max(1, Math.min(limit, 20));
        Pageable pageable = PageRequest.of(0, safeLimit);
        List<Object[]> topProducts = saleItemRepository.findTopSellingProducts(pharmacyId, pageable);

        return topProducts.stream()
                .map(obj -> {
                    Map<String, Object> productMap = new LinkedHashMap<>();
                    productMap.put("productId", obj[0]);
                    productMap.put("productName", obj[1]);
                    productMap.put("totalQuantity", convertToLong(obj[2]));
                    productMap.put("totalRevenue", obj[3]);
                    return productMap;
                })
                .collect(Collectors.toList());
    }

    private Pageable createPageable(String sortBy, String sortDirection, int page, int size) {
        Sort sort = createSort(sortBy, sortDirection);
        return PageRequest.of(page, size, sort);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        List<String> allowedFields = Arrays.asList("transactionDate", "totalAmount", "invoiceNumber", "id");
        if (!allowedFields.contains(sortBy)) {
            sortBy = "transactionDate";
        }
        return Sort.by(direction, sortBy);
    }

    private SaleItem processSaleItem(SaleItemRequest itemRequest, SaleTransaction sale) {
        Product product = productRepository.findById(itemRequest.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + itemRequest.getProductId()));

        StockBatch selectedBatch = selectStockBatch(product.getId(), itemRequest.getQuantity());
        if (selectedBatch == null) {
            throw new RuntimeException("Insufficient stock for product: " + product.getName());
        }

        SaleItem saleItem = SaleItem.builder()
                .product(product)
                .batch(selectedBatch)
                .quantity(itemRequest.getQuantity())
                .unitPrice(itemRequest.getUnitPrice())
                .transaction(sale)
                .build();

        saleItem.calculateTotalPrice();
        deductStock(selectedBatch, itemRequest.getQuantity());
        return saleItem;
    }

    private StockBatch selectStockBatch(Long productId, Integer requestedQuantity) {
        List<StockBatch> activeBatches = stockBatchRepository.findByProductIdAndStatusActive(productId);
        if (activeBatches == null || activeBatches.isEmpty()) {
            return null;
        }
        activeBatches.sort(Comparator.comparing(StockBatch::getExpiryDate));

        Integer remainingQuantity = requestedQuantity;
        StockBatch selectedBatch = null;

        for (StockBatch batch : activeBatches) {
            Integer available = Optional.ofNullable(batch.getQuantityCurrent()).orElse(0);
            if (available >= remainingQuantity) {
                selectedBatch = batch;
                break;
            } else if (available > 0) {
                remainingQuantity -= available;
                if (selectedBatch == null) {
                    selectedBatch = batch;
                }
            }
        }
        return selectedBatch;
    }

    private void deductStock(StockBatch batch, Integer quantity) {
        if (batch == null || batch.getQuantityCurrent() == null) {
            return;
        }
        Integer newQuantity = batch.getQuantityCurrent() - quantity;
        if (newQuantity < 0) {
            throw new RuntimeException("Stock deduction error: insufficient quantity");
        }
        batch.setQuantityCurrent(newQuantity);
        stockBatchRepository.save(batch);
        log.debug("Stock deducted | batch: {} | quantity: {} | remaining: {}",
                batch.getId(), quantity, newQuantity);
    }

    private String generateInvoiceNumber() {
        return "INV-" + System.currentTimeMillis() + "-" +
                String.format("%04d", new Random().nextInt(10000));
    }

    private void validatePharmacyAccess(SaleTransaction sale, Long pharmacyId) {
        if (!sale.getPharmacy().getId().equals(pharmacyId)) {
            log.warn("Access denied | sale: {} | requested pharmacy: {} | actual pharmacy: {}",
                    sale.getId(), pharmacyId, sale.getPharmacy().getId());
            throw new RuntimeException("Access denied: Sale does not belong to this pharmacy");
        }
    }

    private Map<String, Object> getSalesForDate(Long pharmacyId, LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.atTime(LocalTime.MAX);

        Long count = Optional.ofNullable(
                saleTransactionRepository.countByPharmacyIdAndDateRange(pharmacyId, startDate, endDate)
        ).orElse(0L);

        BigDecimal total = Optional.ofNullable(
                saleTransactionRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate)
        ).orElse(BigDecimal.ZERO);

        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total);
        response.put("count", count);
        response.put("date", date);
        return response;
    }

    private List<SalesReportResponse.TopProductDTO> fetchTopProducts(Long pharmacyId,
                                                                     LocalDateTime start,
                                                                     LocalDateTime end,
                                                                     int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> data = saleTransactionRepository.getTopProducts(pharmacyId, start, end, pageable);

        return data.stream()
                .map(obj -> SalesReportResponse.TopProductDTO.builder()
                        .productId((Long) obj[0])
                        .productName((String) obj[1])
                        .quantitySold(convertToLong(obj[2]))
                        .totalRevenue((BigDecimal) obj[3])
                        .build())
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> fetchRevenueByPaymentMethod(Long pharmacyId,
                                                                LocalDateTime start,
                                                                LocalDateTime end) {
        List<Object[]> data = saleTransactionRepository.getRevenueByPaymentMethod(pharmacyId, start, end);
        return data.stream()
                .collect(Collectors.toMap(
                        obj -> ((SaleTransaction.PaymentMethod) obj[0]).name(),
                        obj -> (BigDecimal) obj[1],
                        BigDecimal::add,
                        LinkedHashMap::new
                ));
    }

    private Map<String, Long> fetchDailyOrders(Long pharmacyId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> data = saleTransactionRepository.getDailySales(pharmacyId, start, end);
        return data.stream()
                .collect(Collectors.toMap(
                        obj -> {
                            if (obj[0] instanceof java.sql.Date) {
                                return ((java.sql.Date) obj[0]).toLocalDate().toString();
                            }
                            return obj[0].toString();
                        },
                        obj -> (Long) obj[2],
                        Long::sum,
                        LinkedHashMap::new
                ));
    }

    private Long convertToLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private SaleTransactionDTO mapToDTO(SaleTransaction sale) {
        if (sale == null) return null;
        return SaleTransactionDTO.builder()
                .id(sale.getId())
                .invoiceNumber(sale.getInvoiceNumber())
                .subtotal(sale.getSubtotal())
                .totalAmount(sale.getTotalAmount())
                .discountAmount(sale.getDiscountAmount())
                .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : null)
                .customerPhone(sale.getCustomerPhone())
                .notes(sale.getNotes())
                .transactionDate(sale.getTransactionDate())
                .pharmacyId(sale.getPharmacy().getId())
                .pharmacyName(sale.getPharmacy().getName())
                .items(Optional.ofNullable(sale.getItems()).orElse(Collections.emptyList()).stream()
                        .map(this::mapItemToDTO)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .build();
    }

    private SaleTransactionDTO.SaleItemDTO mapItemToDTO(SaleItem item) {
        try {
            if (item == null) return null;

            String productName = "منتج غير متاح";
            String productBarcode = null;
            Long productId = null;
            String productCategory = null;

            try {
                Product product = item.getProduct();
                if (product != null) {
                    if (!Hibernate.isInitialized(product)) {
                        try {
                            Hibernate.initialize(product);
                        } catch (Exception e) {
                            log.warn("Could not initialize product for sale item {}: {}",
                                    item.getId(), e.getMessage());
                        }
                    }

                    if (product.getDeletedAt() == null) {
                        productId = product.getId();
                        productName = product.getName() != null ?
                                product.getName() : "منتج غير متاح";
                        productBarcode = product.getBarcode();
                        productCategory = product.getCategory();
                    }
                }
            } catch (Exception e) {
                log.warn("Error loading product for sale item {}: {}", item.getId(), e.getMessage());
            }

            return SaleTransactionDTO.SaleItemDTO.builder()
                    .id(item.getId())
                    .productId(productId)
                    .productName(productName)
                    .category(productCategory)
                    .barcode(productBarcode)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .batchNumber(item.getBatch() != null ? item.getBatch().getBatchNumber() : null)
                    .expiryDate(item.getBatch() != null ? item.getBatch().getExpiryDate() : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to map sale item {} to DTO", item != null ? item.getId() : "unknown", e);
            return SaleTransactionDTO.SaleItemDTO.builder()
                    .id(item != null ? item.getId() : null)
                    .productName("خطأ في تحميل البيانات")
                    .build();
        }
    }
}