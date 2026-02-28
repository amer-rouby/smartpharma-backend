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
        return productRepository.findActiveProductsByPharmacy(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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
                    .location("Shelf-1")
                    .status(StockBatch.BatchStatus.ACTIVE)
                    .build();
            stockBatchRepository.save(batch);
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
        product.setBarcode(request.getBarcode());
        product.setCategory(request.getCategory());
        product.setUnitType(request.getUnitType());
        product.setMinStockLevel(request.getMinStockLevel());
        product.setPrescriptionRequired(request.getPrescriptionRequired());
        product.setSellPrice(request.getSellPrice());
        product.setBuyPrice(request.getBuyPrice());
        product.setExtraAttributes(request.getExtraAttributes());

        productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long pharmacyId) {
        Product product = productRepository.findByIdAndPharmacyId(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product.setDeletedAt(java.time.LocalDateTime.now());
        productRepository.save(product);
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