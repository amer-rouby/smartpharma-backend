package com.smartpharma.controller;

import com.smartpharma.dto.request.PurchaseOrderRequest;
import com.smartpharma.dto.response.ApiResponse;
import com.smartpharma.dto.response.PurchaseOrderResponse;
import com.smartpharma.entity.User;
import com.smartpharma.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> getAllOrders(
            @RequestParam Long pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/purchase-orders - pharmacyId: {}, page: {}, size: {}", pharmacyId, page, size);
        Page<PurchaseOrderResponse> orders = purchaseOrderService.getAllOrders(pharmacyId, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> getOrdersByStatus(
            @RequestParam Long pharmacyId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/purchase-orders/status/{} - pharmacyId: {}", status, pharmacyId);
        Page<PurchaseOrderResponse> orders = purchaseOrderService.getOrdersByStatus(pharmacyId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getOrder(
            @PathVariable Long id,
            @RequestParam Long pharmacyId) {
        log.info("GET /api/purchase-orders/{} - pharmacyId: {}", id, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.getOrder(id, pharmacyId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/purchase-orders - pharmacyId: {}, userId: {}", pharmacyId, userId);
        PurchaseOrderResponse order = purchaseOrderService.createOrder(request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order created successfully"));
    }

    @PostMapping("/from-prediction/{predictionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createFromPrediction(
            @PathVariable Long predictionId,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/purchase-orders/from-prediction/{} - pharmacyId: {}", predictionId, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.createFromPrediction(predictionId, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order created from prediction"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("PUT /api/purchase-orders/{} - pharmacyId: {}", id, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.updateOrder(id, request, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("DELETE /api/purchase-orders/{} - pharmacyId: {}", id, pharmacyId);
        purchaseOrderService.deleteOrder(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase order deleted successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approveOrder(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/approve - pharmacyId: {}", id, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.approveOrder(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order approved"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/cancel - pharmacyId: {}", id, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.cancelOrder(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order cancelled"));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveOrder(
            @PathVariable Long id,
            @RequestParam Long pharmacyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/receive - pharmacyId: {}", id, pharmacyId);
        PurchaseOrderResponse order = purchaseOrderService.receiveOrder(id, pharmacyId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order received"));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countOrders(@RequestParam Long pharmacyId) {
        Long total = purchaseOrderService.countOrders(pharmacyId);
        Long draft = purchaseOrderService.countOrdersByStatus(pharmacyId, "DRAFT");
        Long pending = purchaseOrderService.countOrdersByStatus(pharmacyId, "PENDING");
        Long approved = purchaseOrderService.countOrdersByStatus(pharmacyId, "APPROVED");
        Long received = purchaseOrderService.countOrdersByStatus(pharmacyId, "RECEIVED");

        Map<String, Long> response = new HashMap<>();
        response.put("total", total);
        response.put("draft", draft);
        response.put("pending", pending);
        response.put("approved", approved);
        response.put("received", received);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getOrdersByDateRange(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("GET /api/purchase-orders/date-range - pharmacyId: {}, start: {}, end: {}", pharmacyId, startDate, endDate);
        List<PurchaseOrderResponse> orders = purchaseOrderService.getOrdersByDateRange(pharmacyId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/total-amount")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalAmount(
            @RequestParam Long pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        BigDecimal total = purchaseOrderService.getTotalPurchasesAmount(pharmacyId, startDate, endDate);
        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total != null ? total : BigDecimal.ZERO);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User user) return user.getId();
        try {
            return Long.valueOf(userDetails.getUsername());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}