package com.smartpharma.service;

import com.smartpharma.dto.request.ProductRequest;
import com.smartpharma.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts(Long pharmacyId);

    Page<ProductResponse> getProductsPage(Long pharmacyId, int page, int size, String search,
                                           String category, String sortBy, String sortDirection);

    Long getProductsCount(Long pharmacyId);

    ProductResponse getProduct(Long id, Long pharmacyId);

    ProductResponse createProduct(ProductRequest request, Long pharmacyId);

    ProductResponse updateProduct(Long id, ProductRequest request, Long pharmacyId);

    void deleteProduct(Long id, Long pharmacyId);

    List<ProductResponse> searchProducts(Long pharmacyId, String query);

    List<ProductResponse> getLowStockProducts(Long pharmacyId);
}