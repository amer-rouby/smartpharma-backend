package com.smartpharma.purchasing.service;

import com.smartpharma.purchasing.dto.request.PurchaseOrderRequest;
import com.smartpharma.purchasing.dto.response.PurchaseOrderResponse;
import com.smartpharma.purchasing.dto.response.SendEmailResponse;
import com.smartpharma.purchasing.dto.response.SendWhatsAppResponse;
import com.smartpharma.purchasing.dto.response.WhatsAppMessageResponse;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;

public interface PurchaseOrderService {
    Page<PurchaseOrderResponse> getAllOrders(Long pharmacyId, int page, int size);
    Page<PurchaseOrderResponse> getOrdersByStatus(Long pharmacyId, String status, int page, int size);
    PurchaseOrderResponse getOrder(Long id, Long pharmacyId);
    PurchaseOrderResponse createOrder(PurchaseOrderRequest request, Long pharmacyId, Long userId);
    PurchaseOrderResponse updateOrder(Long id, PurchaseOrderRequest request, Long pharmacyId, Long userId);
    void deleteOrder(Long id, Long pharmacyId, Long userId);
    PurchaseOrderResponse approveOrder(Long id, Long pharmacyId, Long userId);
    PurchaseOrderResponse cancelOrder(Long id, Long pharmacyId, Long userId);
    PurchaseOrderResponse receiveOrder(Long id, Long pharmacyId, Long userId);
    Long countOrders(Long pharmacyId);
    Long countOrdersByStatus(Long pharmacyId, String status);
    List<PurchaseOrderResponse> getOrdersByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate);
    java.math.BigDecimal getTotalPurchasesAmount(Long pharmacyId, LocalDate startDate, LocalDate endDate);
    PurchaseOrderResponse createFromPrediction(Long predictionId, Long pharmacyId, Long userId);
    WhatsAppMessageResponse generateWhatsAppMessage(Long orderId, Long pharmacyId);
    SendWhatsAppResponse sendWhatsAppMessage(Long orderId, Long pharmacyId);
    SendEmailResponse sendPurchaseOrderEmail(Long orderId, Long pharmacyId);
}
