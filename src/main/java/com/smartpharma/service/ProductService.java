package com.smartpharma.service;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ProductResponse;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.Product;
import com.smartpharma.repository.PharmacyRepository;
import com.smartpharma.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(Long pharmacyId) {
        return productRepository.findActiveProductsByPharmacy(pharmacyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id, Long pharmacyId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request, Long pharmacyId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        if (request.getBarcode() != null &&
                productRepository.existsByPharmacyIdAndBarcode(pharmacyId, request.getBarcode())) {
            throw new RuntimeException("Barcode already exists");
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
        return mapToResponse(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request, Long pharmacyId) {
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
        return mapToResponse(product);
    }

    @Transactional
    public void deleteProduct(Long id, Long pharmacyId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!product.getPharmacy().getId().equals(pharmacyId)) {
            throw new RuntimeException("Access denied");
        }

        product.setDeletedAt(java.time.LocalDateTime.now());
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(Long pharmacyId, String query) {
        return productRepository.findByPharmacyIdAndNameContainingIgnoreCase(pharmacyId, query)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

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