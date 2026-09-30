package com.smartpharma.catalog.service.impl;

import com.smartpharma.catalog.dto.request.BulkPriceUpdateRequest;
import com.smartpharma.catalog.dto.request.ProductRequest;
import com.smartpharma.catalog.dto.response.BulkPriceUpdateResponse;
import com.smartpharma.catalog.dto.response.ProductResponse;
import com.smartpharma.common.entity.Pharmacy;
import com.smartpharma.catalog.entity.Product;
import com.smartpharma.catalog.entity.StockBatch;
import com.smartpharma.common.repository.PharmacyRepository;
import com.smartpharma.catalog.repository.ProductRepository;
import com.smartpharma.catalog.repository.StockBatchRepository;
import com.smartpharma.catalog.service.ProductService;
import com.smartpharma.common.exception.LocalizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(Long pharmacyId) {
        return productRepository.findActiveProductsByPharmacy(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsPage(Long pharmacyId, int page, int size, String search,
                                                  String category, String sortBy, String sortDirection) {
        Pageable pageable = createPageable(page, size, sortBy, sortDirection);
        return productRepository.searchAndFilter(pharmacyId, search, category, pageable)
                .map(this::mapToResponse);
    }

    private Pageable createPageable(int page, int size, String sortBy, String sortDirection) {
        List<String> allowedFields = Arrays.asList("name", "sellPrice", "category", "createdAt");
        String safeSortBy = allowedFields.contains(sortBy) ? sortBy : "name";
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(direction, safeSortBy));
    }

    @Override
    @Transactional(readOnly = true)
    public Long getProductsCount(Long pharmacyId) {
        return productRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id, Long pharmacyId) {
        Product product = productRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        if (request.getBarcode() != null && !request.getBarcode().trim().isEmpty()) {
            if (productRepository.findByPharmacyIdAndBarcode(pharmacyId, request.getBarcode().trim()).isPresent()) {
                throw new RuntimeException("Barcode already exists");
            }
        }

        Product product = Product.builder()
                .pharmacy(pharmacy)
                .name(request.getName())
                .scientificName(request.getScientificName())
                .activeIngredient(blankToNull(request.getActiveIngredient()))
                .barcode(request.getBarcode())
                .category(request.getCategory())
                .etaItemType(blankToNull(request.getEtaItemType()))
                .etaItemCode(blankToNull(request.getEtaItemCode()))
                .etaTaxSubtype(blankToNull(request.getEtaTaxSubtype()))
                .etaTaxRate("V010".equals(request.getEtaTaxSubtype()) ? request.getEtaTaxRate() : null)
                .unitType(request.getUnitType())
                .minStockLevel(request.getMinStockLevel())
                .prescriptionRequired(request.getPrescriptionRequired())
                .sellPrice(request.getSellPrice())
                .buyPrice(request.getBuyPrice())
                .extraAttributes(request.getExtraAttributes())
                .build();

        productRepository.save(product);
        log.info("Product created: id={}, name={}", product.getId(), product.getName());
        if (request.getInitialStock() != null && request.getInitialStock() > 0) {
            BigDecimal effectiveBuyPrice = request.getBuyPrice() != null
                    ? request.getBuyPrice()
                    : request.getSellPrice();

            LocalDate expiryDate = request.getExpiryDate() != null
                    ? request.getExpiryDate()
                    : LocalDate.now().plusMonths(24);

            StockBatch batch = StockBatch.builder()
                    .product(product)
                    .pharmacy(pharmacy)
                    .batchNumber("BATCH-" + product.getId() + "-" + System.currentTimeMillis())
                    .quantityInitial(request.getInitialStock())
                    .quantityCurrent(request.getInitialStock())
                    .expiryDate(expiryDate)
                    .buyPrice(effectiveBuyPrice)
                    .sellPrice(request.getSellPrice())
                    .location("Shelf-1")
                    .status(StockBatch.BatchStatus.ACTIVE)
                    .build();
            stockBatchRepository.save(batch);
            // product.stockBatches is a plain empty ArrayList on this just-built entity
            // (not a Hibernate lazy proxy, since it was never loaded from the DB), so it
            // won't pick up the batch we just saved unless we add it ourselves - without
            // this, getTotalStock() below reports 0 even though the batch exists in the DB.
            product.getStockBatches().add(batch);
            log.info("Initial stock batch created: productId={}, quantity={}, buyPrice={}",
                    product.getId(), request.getInitialStock(), effectiveBuyPrice);
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long pharmacyId) {
        Product product = productRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setName(request.getName());
        product.setScientificName(request.getScientificName());
        if (request.getActiveIngredient() != null) {
            product.setActiveIngredient(blankToNull(request.getActiveIngredient()));
        }
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        if (request.getEtaItemType() != null) product.setEtaItemType(blankToNull(request.getEtaItemType()));
        if (request.getEtaItemCode() != null) product.setEtaItemCode(blankToNull(request.getEtaItemCode()));
        if (request.getEtaTaxSubtype() != null) {
            product.setEtaTaxSubtype(blankToNull(request.getEtaTaxSubtype()));
            // A rate only means something for V010 ("other rate").
            product.setEtaTaxRate("V010".equals(request.getEtaTaxSubtype()) ? request.getEtaTaxRate() : null);
        }
        product.setUnitType(request.getUnitType());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setPrescriptionRequired(request.getPrescriptionRequired());

        if (request.getBuyPrice() != null) {
            product.setBuyPrice(request.getBuyPrice());

            if (request.getSellPrice() == null) {
                BigDecimal profitMargin = BigDecimal.valueOf(0.25);
                BigDecimal autoSellPrice = request.getBuyPrice()
                        .multiply(BigDecimal.ONE.add(profitMargin))
                        .setScale(2, RoundingMode.HALF_UP);
                product.setSellPrice(autoSellPrice);
                log.info("Auto-calculated sell price: buyPrice={}, sellPrice={}, margin=25%",
                        request.getBuyPrice(), autoSellPrice);
            } else {
                product.setSellPrice(request.getSellPrice());
            }
        } else if (request.getSellPrice() != null) {
            product.setSellPrice(request.getSellPrice());
        }

        product.setExtraAttributes(request.getExtraAttributes());

        // Flush so @PreUpdate re-derives ingredientKey before the response is built.
        Product updated = productRepository.saveAndFlush(product);
        log.info("Product updated: id={}, name={}, buyPrice={}, sellPrice={}",
                updated.getId(), updated.getName(), updated.getBuyPrice(), updated.getSellPrice());

        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long pharmacyId) {
        Product product = productRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getDeletedAt() != null) {
            log.warn("Product already deleted: id={}", id);
            return;
        }

        product.setDeletedAt(LocalDateTime.now());
        productRepository.save(product);
        log.info("Product soft deleted: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(Long pharmacyId, String query) {
        return productRepository.findByPharmacyIdAndNameContainingIgnoreCase(pharmacyId, query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Long pharmacyId) {
        return productRepository.findLowStockProducts(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProductResponse mapToResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .pharmacyId(product.getPharmacy() != null ? product.getPharmacy().getId() : null)
                .name(product.getName())
                .scientificName(product.getScientificName())
                .activeIngredient(product.getActiveIngredient())
                .ingredientKey(product.getIngredientKey())
                .barcode(product.getBarcode())
                .category(product.getCategory())
                .etaItemType(product.getEtaItemType())
                .etaItemCode(product.getEtaItemCode())
                .etaTaxSubtype(product.getEtaTaxSubtype())
                .etaTaxRate(product.getEtaTaxRate())
                .unitType(product.getUnitType())
                .minStockLevel(product.getMinStockLevel())
                .prescriptionRequired(product.getPrescriptionRequired())
                .sellPrice(product.getSellPrice())
                .buyPrice(product.getBuyPrice())
                .extraAttributes(product.getExtraAttributes())
                .totalStock(product.getTotalStock())
                .createdAt(product.getCreatedAt())
                .build();
    }

    // Previews (apply=false) or applies many sell-price changes at once. The
    // preview and the apply compute the same thing, so what the admin reviewed
    // is exactly what gets saved.
    @Override
    @Transactional
    public BulkPriceUpdateResponse updatePricesInBulk(BulkPriceUpdateRequest request, Long pharmacyId) {
        // Keyed by id: Product's Lombok hashCode covers its batches (which point
        // back at the product) and changes when the price is set.
        Map<Long, PriceTarget> newPrices = new LinkedHashMap<>();
        List<String> notFound = new ArrayList<>();

        if ("PERCENT".equals(request.getMode())) {
            if (request.getPercent() == null) {
                throw new LocalizedException(HttpStatus.BAD_REQUEST, "BULK_PRICE_PERCENT_REQUIRED",
                        "A percentage is required");
            }
            BigDecimal factor = BigDecimal.valueOf(100).add(request.getPercent())
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            for (Product product : percentTargets(request, pharmacyId)) {
                BigDecimal current = product.getSellPrice() != null ? product.getSellPrice() : BigDecimal.ZERO;
                newPrices.put(product.getId(), new PriceTarget(product, roundPrice(current.multiply(factor), request.getRoundTo())));
            }
        } else {
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new LocalizedException(HttpStatus.BAD_REQUEST, "BULK_PRICE_ITEMS_REQUIRED",
                        "The price list is empty");
            }
            List<Product> products = productRepository.findByPharmacyId(pharmacyId);
            Map<Long, Product> byId = products.stream().collect(Collectors.toMap(Product::getId, p -> p));
            Map<String, Product> byBarcode = products.stream()
                    .filter(p -> p.getBarcode() != null && !p.getBarcode().isBlank())
                    .collect(Collectors.toMap(p -> p.getBarcode().trim(), p -> p, (a, b) -> a));
            for (BulkPriceUpdateRequest.PriceListItem item : request.getItems()) {
                Product product = item.getProductId() != null ? byId.get(item.getProductId())
                        : item.getBarcode() != null ? byBarcode.get(item.getBarcode().trim()) : null;
                if (product == null) {
                    notFound.add(item.getProductId() != null ? "#" + item.getProductId() : String.valueOf(item.getBarcode()));
                    continue;
                }
                newPrices.put(product.getId(), new PriceTarget(product, item.getSellPrice().setScale(2, RoundingMode.HALF_UP)));
            }
        }

        List<BulkPriceUpdateResponse.PriceChange> changes = new ArrayList<>();
        int unchanged = 0;
        for (PriceTarget target : newPrices.values()) {
            Product product = target.product();
            BigDecimal oldPrice = product.getSellPrice();
            BigDecimal newPrice = target.newPrice();
            if (oldPrice != null && oldPrice.compareTo(newPrice) == 0) {
                unchanged++;
                continue;
            }
            changes.add(BulkPriceUpdateResponse.PriceChange.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .barcode(product.getBarcode())
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .belowCost(product.getBuyPrice() != null && newPrice.compareTo(product.getBuyPrice()) < 0)
                    .build());
            if (request.isApply()) {
                product.setSellPrice(newPrice);
            }
        }

        if (request.isApply() && !changes.isEmpty()) {
            productRepository.saveAll(newPrices.values().stream().map(PriceTarget::product).toList());
            log.info("Bulk price update | pharmacy {} | mode {} | {} product(s) changed", pharmacyId,
                    request.getMode(), changes.size());
        }
        return BulkPriceUpdateResponse.builder()
                .applied(request.isApply())
                .changedCount(changes.size())
                .unchangedCount(unchanged)
                .changes(changes)
                .notFound(notFound)
                .build();
    }

    private record PriceTarget(Product product, BigDecimal newPrice) {
    }

    private List<Product> percentTargets(BulkPriceUpdateRequest request, Long pharmacyId) {
        List<Product> all = productRepository.findByPharmacyId(pharmacyId);
        if (request.getProductIds() != null && !request.getProductIds().isEmpty()) {
            Set<Long> ids = new HashSet<>(request.getProductIds());
            return all.stream().filter(p -> ids.contains(p.getId())).toList();
        }
        String category = blankToNull(request.getCategory());
        if (category != null) {
            return all.stream().filter(p -> category.equalsIgnoreCase(p.getCategory())).toList();
        }
        return all;
    }

    // Rounds to the nearest multiple of step (e.g. 0.25), never below 0.01.
    static BigDecimal roundPrice(BigDecimal price, BigDecimal step) {
        BigDecimal rounded = step == null || step.signum() <= 0
                ? price.setScale(2, RoundingMode.HALF_UP)
                : price.divide(step, 0, RoundingMode.HALF_UP).multiply(step).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minimum = new BigDecimal("0.01");
        return rounded.compareTo(minimum) < 0 ? minimum : rounded;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}