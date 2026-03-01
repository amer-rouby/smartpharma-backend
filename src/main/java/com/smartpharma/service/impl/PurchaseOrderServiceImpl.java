package com.smartpharma.service.impl;

import com.smartpharma.dto.request.PurchaseOrderItemRequest;
import com.smartpharma.dto.request.PurchaseOrderRequest;
import com.smartpharma.dto.response.PurchaseOrderResponse;
import com.smartpharma.entity.*;
import com.smartpharma.repository.*;
import com.smartpharma.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final UserRepository userRepository;
    private final StockBatchRepository stockBatchRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllOrders(Long pharmacyId, int page, int size) {
        return orderRepository.findByPharmacyId(pharmacyId, PageRequest.of(page, size))
                .map(PurchaseOrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getOrdersByStatus(Long pharmacyId, String status, int page, int size) {
        return orderRepository.findByPharmacyIdAndStatus(pharmacyId, status, PageRequest.of(page, size))
                .map(PurchaseOrderResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseOrderResponse getOrder(Long id, Long pharmacyId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));
        return PurchaseOrderResponse.fromEntity(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createOrder(PurchaseOrderRequest request, Long pharmacyId, Long userId) {
        Pharmacy pharmacy = pharmacyRepository.findById(pharmacyId)
                .orElseThrow(() -> new RuntimeException("Pharmacy not found"));

        Supplier supplier = supplierRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(request.getSupplierId(), pharmacyId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String orderNumber = generateOrderNumber(pharmacyId);

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(orderNumber)
                .pharmacy(pharmacy)
                .supplier(supplier)
                .orderDate(request.getOrderDate())
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .totalAmount(java.math.BigDecimal.ZERO)
                .status("DRAFT")
                .priority(request.getPriority())
                .paymentTerms(request.getPaymentTerms())
                .notes(request.getNotes())
                .sourceType(request.getSourceType())
                .sourceId(request.getSourceId())
                .createdBy(user)
                .build();

        // Add items
        // في method createOrder، عدّل الجزء الخاص بـ items كالتالي:

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndPharmacyId(itemReq.getProductId(), pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            // ✅ FIXED: Validate unitPrice
            BigDecimal unitPrice = itemReq.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Invalid unit price for product: " + product.getName());
            }

            // ✅ Validate quantity
            Integer quantity = itemReq.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new RuntimeException("Invalid quantity for product: " + product.getName());
            }

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)  // ✅ مؤكد إنه مش null
                    .notes(itemReq.getNotes())
                    .build();

            // ✅ احسب totalPrice قبل الـ add
            item.calculateTotal();

            order.addItem(item);  // ✅ ده هيستدعي recalculateTotal() الآمنة
        }
        PurchaseOrder saved = orderRepository.save(order);
        log.info("Purchase order created: number={}, supplier={}, total={}",
                saved.getOrderNumber(), supplier.getName(), saved.getTotalAmount());
        return PurchaseOrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createFromPrediction(Long predictionId, Long pharmacyId, Long userId) {
        // Check if order already exists for this prediction
        List<PurchaseOrder> existing = orderRepository.findByPredictionId(predictionId);
        if (!existing.isEmpty()) {
            throw new RuntimeException("Purchase order already exists for this prediction");
        }

        // Get prediction details (you'll need to call prediction service or repository)
        // For now, create a basic order - you can enhance this later
        PurchaseOrderRequest request = PurchaseOrderRequest.builder()
                .supplierId(null) // User will select supplier
                .orderDate(LocalDate.now())
                .sourceType("PREDICTION")
                .sourceId(predictionId)
                .items(List.of()) // User will add items
                .build();

        return createOrder(request, pharmacyId, userId);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse updateOrder(Long id, PurchaseOrderRequest request, Long pharmacyId, Long userId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (!order.isDraft()) {
            throw new RuntimeException("Only draft orders can be updated");
        }

        Supplier supplier = supplierRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(request.getSupplierId(), pharmacyId)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));

        order.setSupplier(supplier);
        order.setOrderDate(request.getOrderDate());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setPriority(request.getPriority());
        order.setPaymentTerms(request.getPaymentTerms());
        order.setNotes(request.getNotes());

        // Clear existing items
        order.getItems().clear();

        // Add new items
        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndPharmacyId(itemReq.getProductId(), pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .notes(itemReq.getNotes())
                    .build();

            order.addItem(item);
        }

        PurchaseOrder updated = orderRepository.save(order);
        log.info("Purchase order updated: number={}", updated.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id, Long pharmacyId, Long userId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (!order.isDraft()) {
            throw new RuntimeException("Only draft orders can be deleted");
        }

        order.setDeletedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Purchase order deleted (soft): id={}", id);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse approveOrder(Long id, Long pharmacyId, Long userId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (!order.isDraft()) {
            throw new RuntimeException("Only draft orders can be approved");
        }

        User approver = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        order.setStatus("APPROVED");
        order.setApprovedBy(approver);

        PurchaseOrder updated = orderRepository.save(order);
        log.info("Purchase order approved: number={}, by={}", updated.getOrderNumber(), userId);
        return PurchaseOrderResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelOrder(Long id, Long pharmacyId, Long userId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (order.isReceived() || order.isCancelled()) {
            throw new RuntimeException("Cannot cancel this order");
        }

        order.setStatus("CANCELLED");
        PurchaseOrder updated = orderRepository.save(order);
        log.info("Purchase order cancelled: number={}", updated.getOrderNumber());
        return PurchaseOrderResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receiveOrder(Long id, Long pharmacyId, Long userId) {
        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(id, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found"));

        if (!order.isApproved()) {
            throw new RuntimeException("Only approved orders can be received");
        }

        order.setStatus("RECEIVED");
        order.setActualDeliveryDate(LocalDate.now());

        // Create stock batches for received items
        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getQuantity() > 0) {
                StockBatch batch = StockBatch.builder()
                        .product(item.getProduct())
                        .pharmacy(order.getPharmacy())
                        .batchNumber("PO-" + order.getOrderNumber() + "-" + item.getProduct().getId())
                        .quantityInitial(item.getQuantity())
                        .quantityCurrent(item.getQuantity())
                        .expiryDate(LocalDate.now().plusMonths(24)) // Default, user can update
                        .buyPrice(item.getUnitPrice())
                        .sellPrice(item.getProduct().getSellPrice())
                        .status(StockBatch.BatchStatus.ACTIVE)
                        .createdBy(User.builder().id(userId).build())
                        .build();

                stockBatchRepository.save(batch);
                item.setReceivedQuantity(item.getQuantity());
            }
        }

        PurchaseOrder updated = orderRepository.save(order);
        log.info("Purchase order received: number={}, items={}", updated.getOrderNumber(), updated.getItems().size());
        return PurchaseOrderResponse.fromEntity(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countOrders(Long pharmacyId) {
        return orderRepository.countByPharmacyId(pharmacyId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countOrdersByStatus(Long pharmacyId, String status) {
        return orderRepository.countByPharmacyIdAndStatus(pharmacyId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> getOrdersByDateRange(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        return orderRepository.findByPharmacyIdAndDateRange(pharmacyId, startDate, endDate).stream()
                .map(PurchaseOrderResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.math.BigDecimal getTotalPurchasesAmount(Long pharmacyId, LocalDate startDate, LocalDate endDate) {
        return orderRepository.sumTotalAmountByPharmacyIdAndDateRange(pharmacyId, startDate, endDate);
    }

    private String generateOrderNumber(Long pharmacyId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        return "PO-" + pharmacyId + "-" + timestamp;
    }
}