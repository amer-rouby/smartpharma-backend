package com.smartpharma.service.impl;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ProductResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.entity.StockBatch;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.StockBatchRepository;
import com.smartpharma.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
        log.debug("Fetching products for pharmacyId: {}", pharmacyId);

        return productRepository.findActiveProductsByPharmacy(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getProductsCount(Long pharmacyId) {
        log.debug("Counting products for pharmacyId: {}", pharmacyId);
        return productRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id, Long pharmacyId) {
        log.debug("Fetching product {} for pharmacyId: {}", id, pharmacyId);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long pharmacyId) {
        log.info("Creating product '{}' for pharmacyId: {}", request.getName(), pharmacyId);

        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        if (request.getBarcode() != null && !request.getBarcode().trim().isEmpty()) {
            if (productRepository.existsByPharmacyIdAndBarcode(pharmacyId, request.getBarcode().trim())) {
                throw new RuntimeException("Barcode already exists for this pharmacy");
            }
        }

        Product product = Product.builder()
                .pharmacy(pharmacy)
                .name(request.getName())
                .scientificName(request.getScientificName())
                .barcode(request.getBarcode())
                .category(request.getCategory())
                .unitType(request.getUnitType())
                .minStockLevel(request.getMinStockLevel())
                .prescriptionRequired(request.getPrescriptionRequired())
                .sellPrice(request.getSellPrice())
                .buyPrice(request.getBuyPrice())
                .extraAttributes(request.getExtraAttributes())
                .build();

        productRepository.save(product);

        // ✅ إنشاء StockBatch تلقائي لو فيه مخزون أولي
        if (request.getInitialStock() != null && request.getInitialStock() > 0) {
            StockBatch batch = StockBatch.builder()
                    .product(product)
                    .pharmacy(pharmacy)
                    .batchNumber("BATCH-" + product.getId() + "-" + System.currentTimeMillis())
                    .quantityInitial(request.getInitialStock())
                    .quantityCurrent(request.getInitialStock())
                    .expiryDate(LocalDate.now().plusMonths(24))
                    .buyPrice(request.getBuyPrice() != null ? request.getBuyPrice() : request.getSellPrice())
                    .sellPrice(request.getSellPrice())
                    .location("رف-1")
                    .status(StockBatch.BatchStatus.ACTIVE)
                    .build();

            stockBatchRepository.save(batch);
            log.info("Stock batch created for product {}: quantity = {}", product.getId(), request.getInitialStock());
        }

        log.info("Product created with ID: {}", product.getId());
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long pharmacyId) {
        log.info("Updating product {} for pharmacyId: {}", id, pharmacyId);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        product.setName(request.getName());
        product.setScientificName(request.getScientificName());
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        product.setUnitType(request.getUnitType());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setPrescriptionRequired(request.getPrescriptionRequired());
        product.setSellPrice(request.getSellPrice());
        product.setBuyPrice(request.getBuyPrice());
        product.setExtraAttributes(request.getExtraAttributes());

        productRepository.save(product);
        log.info("Product updated with ID: {}", product.getId());

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long pharmacyId) {
        log.info("Soft deleting product {} for pharmacyId: {}", id, pharmacyId);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        product.setDeletedAt(java.time.LocalDateTime.now());
        productRepository.save(product);
        log.info("Product soft-deleted with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(Long pharmacyId, String query) {
        log.debug("Searching products for pharmacyId: {}, query: '{}'", pharmacyId, query);

        return productRepository.findByPharmacyIdAndNameContainingIgnoreCase(pharmacyId, query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Long pharmacyId) {
        log.debug("Fetching low stock products for pharmacyId: {}", pharmacyId);

        return productRepository.findLowStockProducts(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 🔧 Helper Method: Entity → DTO Mapping
    // ==========================================

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .pharmacyId(product.getPharmacy().getId())
                .name(product.getName())
                .scientificName(product.getScientificName())
                .barcode(product.getBarcode())
                .category(product.getCategory())
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
}