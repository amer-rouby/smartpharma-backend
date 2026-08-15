package com.smartpharma.service.impl;

import com.smartpharma.dto.request.PurchaseOrderItemRequest;
import com.smartpharma.dto.request.PurchaseOrderRequest;
import com.smartpharma.dto.request.StockMovementRequest;
import com.smartpharma.dto.response.PurchaseOrderResponse;
import com.smartpharma.dto.response.SendEmailResponse;
import com.smartpharma.dto.response.SendWhatsAppResponse;
import com.smartpharma.dto.response.WhatsAppMessageResponse;
import com.smartpharma.entity.*;
import com.smartpharma.repository.*;
import com.smartpharma.service.EmailService;
import com.smartpharma.service.PurchaseOrderPdfService;
import com.smartpharma.service.PurchaseOrderService;
import com.smartpharma.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final StockMovementService stockMovementService;
    private final EmailService emailService;
    private final PurchaseOrderPdfService purchaseOrderPdfService;

    @Override
    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllOrders(Long pharmacyId, int page, int size) {
        log.info("Fetching purchase orders for pharmacy: {}, page: {}, size: {}", pharmacyId, page, size);
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

        if (!supplier.isActive()) {
            throw new RuntimeException("Cannot create a purchase order for a blocked or inactive supplier: " + supplier.getName());
        }

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

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findByIdAndPharmacyId(itemReq.getProductId(), pharmacyId)
                    .orElseThrow(() -> new RuntimeException("Product not found: " + itemReq.getProductId()));

            BigDecimal unitPrice = itemReq.getUnitPrice();
            if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Invalid unit price for product: " + product.getName());
            }

            Integer quantity = itemReq.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new RuntimeException("Invalid quantity for product: " + product.getName());
            }

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .notes(itemReq.getNotes())
                    .build();

            item.calculateTotal();
            order.addItem(item);
        }

        PurchaseOrder saved = orderRepository.save(order);
        log.info("Purchase order created: number={}, supplier={}, total={}",
                saved.getOrderNumber(), supplier.getName(), saved.getTotalAmount());
        return PurchaseOrderResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse createFromPrediction(Long predictionId, Long pharmacyId, Long userId) {
        List<PurchaseOrder> existing = orderRepository.findByPredictionId(predictionId);
        if (!existing.isEmpty()) {
            throw new RuntimeException("Purchase order already exists for this prediction");
        }

        PurchaseOrderRequest request = PurchaseOrderRequest.builder()
                .supplierId(null)
                .orderDate(LocalDate.now())
                .sourceType("PREDICTION")
                .sourceId(predictionId)
                .items(List.of())
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

        if (!supplier.isActive()) {
            throw new RuntimeException("Cannot assign a blocked or inactive supplier: " + supplier.getName());
        }

        order.setSupplier(supplier);
        order.setOrderDate(request.getOrderDate());
        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setPriority(request.getPriority());
        order.setPaymentTerms(request.getPaymentTerms());
        order.setNotes(request.getNotes());

        order.getItems().clear();

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

        for (PurchaseOrderItem item : order.getItems()) {
            if (item.getQuantity() > 0) {
                Product product = item.getProduct();
                BigDecimal newBuyPrice = item.getUnitPrice();

                StockBatch batch = StockBatch.builder()
                        .product(product)
                        .pharmacy(order.getPharmacy())
                        .batchNumber("PO-" + order.getOrderNumber() + "-" + item.getProduct().getId())
                        .quantityInitial(item.getQuantity())
                        .quantityCurrent(item.getQuantity())
                        .expiryDate(LocalDate.now().plusMonths(24))
                        .buyPrice(newBuyPrice)
                        .sellPrice(product.getSellPrice())
                        .status(StockBatch.BatchStatus.ACTIVE)
                        .createdBy(User.builder().id(userId).build())
                        .build();

                stockBatchRepository.save(batch);
                item.setReceivedQuantity(item.getQuantity());

                if (product.getBuyPrice() == null || product.getBuyPrice().compareTo(newBuyPrice) != 0) {
                    BigDecimal oldBuyPrice = product.getBuyPrice();
                    product.setBuyPrice(newBuyPrice);

                    BigDecimal profitMargin = BigDecimal.valueOf(0.25);
                    BigDecimal newSellPrice = newBuyPrice.multiply(BigDecimal.ONE.add(profitMargin))
                            .setScale(2, BigDecimal.ROUND_HALF_UP);

                    if (product.getSellPrice() == null || product.getSellPrice().compareTo(newSellPrice) != 0) {
                        product.setSellPrice(newSellPrice);
                    }

                    productRepository.save(product);

                    log.info("Product prices updated | productId: {} | productName: {} | oldBuyPrice: {} | newBuyPrice: {} | newSellPrice: {}",
                            product.getId(), product.getName(), oldBuyPrice, newBuyPrice, newSellPrice);
                }

                try {
                    StockMovementRequest movementRequest = StockMovementRequest.builder()
                            .batchId(batch.getId())
                            .movementType(StockMovement.MovementType.STOCK_IN)
                            .quantity(item.getQuantity())
                            .unitPrice(newBuyPrice)
                            .referenceNumber(order.getOrderNumber())
                            .reason("Purchase order received: " + order.getOrderNumber())
                            .build();

                    stockMovementService.createMovement(movementRequest, userId, pharmacyId);
                    log.info("Stock movement created for received order: batchId={}, quantity={}",
                            batch.getId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Failed to create stock movement for received order: {}", e.getMessage());
                }
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

    @Override
    @Transactional
    public SendWhatsAppResponse sendWhatsAppMessage(Long orderId, Long pharmacyId) {
        log.info("Sending WhatsApp message for orderId: {}, pharmacyId: {}", orderId, pharmacyId);

        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(orderId, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + orderId));

        Supplier supplier = order.getSupplier();
        if (supplier == null) {
            throw new RuntimeException("No supplier assigned to this purchase order");
        }

        String phone = supplier.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Supplier has no phone number");
        }

        // Clean phone number to international format (without +)
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.startsWith("002")) {
            cleanPhone = cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("00")) {
            cleanPhone = cleanPhone.substring(2);
        } else if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1);
        }
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "20" + cleanPhone.substring(1);
        }

        String message = formatWhatsAppMessage(order);

        // URL-encode the message for wa.me link
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;

        log.info("WhatsApp Click-to-Chat link generated for order: {} -> phone: {}", order.getOrderNumber(), cleanPhone);

        return SendWhatsAppResponse.builder()
                .success(true)
                .messageId("click-to-chat")
                .whatsAppUrl(whatsappUrl)
                .phoneNumber(cleanPhone)
                .encodedMessage(encodedMessage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WhatsAppMessageResponse generateWhatsAppMessage(Long orderId, Long pharmacyId) {
        log.info("Generating WhatsApp message for orderId: {}, pharmacyId: {}", orderId, pharmacyId);

        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(orderId, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + orderId));

        Supplier supplier = order.getSupplier();
        if (supplier == null) {
            throw new RuntimeException("No supplier assigned to this purchase order");
        }

        String phone = supplier.getPhone();
        if (phone == null || phone.isBlank()) {
            throw new RuntimeException("Supplier has no phone number");
        }

        // Clean phone number: keep only digits, ensure international format without +
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        if (cleanPhone.startsWith("002")) {
            cleanPhone = cleanPhone.substring(3);
        } else if (cleanPhone.startsWith("00")) {
            cleanPhone = cleanPhone.substring(2);
        } else if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1);
        }
        // If it starts with 0 (Egyptian local format), replace 0 with 20
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "20" + cleanPhone.substring(1);
        }

        String message = formatWhatsAppMessage(order);

        // URL-encode the message for wa.me link
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);
        String whatsappUrl = "https://wa.me/" + cleanPhone + "?text=" + encodedMessage;

        log.info("WhatsApp message generated for order: {} -> phone: {}", order.getOrderNumber(), cleanPhone);

        return WhatsAppMessageResponse.builder()
                .phoneNumber(cleanPhone)
                .message(message)
                .encodedMessage(encodedMessage)
                .whatsAppUrl(whatsappUrl)
                .orderNumber(order.getOrderNumber())
                .build();
    }

    private String formatWhatsAppMessage(PurchaseOrder order) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("*📋 طلب شراء جديد*\n");
        sb.append("*Purchase Order* 🤝\n\n");

        // Pharmacy info (sender)
        Pharmacy pharmacy = order.getPharmacy();
        if (pharmacy != null) {
            sb.append("*🏥 الصيدلية / Pharmacy:* ");
            sb.append(pharmacy.getName() != null ? pharmacy.getName() : "N/A").append("\n");
            if (pharmacy.getAddress() != null && !pharmacy.getAddress().isBlank()) {
                sb.append("*📍 العنوان / Address:* ");
                sb.append(pharmacy.getAddress()).append("\n");
            }
            if (pharmacy.getPhone() != null && !pharmacy.getPhone().isBlank()) {
                sb.append("*📞 الهاتف / Phone:* ");
                sb.append(pharmacy.getPhone()).append("\n");
            }
            sb.append("\n");
        }

        // Order details
        sb.append("*📄 رقم الطلب / Order No:* ");
        sb.append(order.getOrderNumber()).append("\n");
        sb.append("*📅 التاريخ / Date:* ");
        sb.append(order.getOrderDate()).append("\n");
        sb.append("*🏪 المورد / Supplier:* ");
        sb.append(order.getSupplier().getName()).append("\n");
        sb.append("*📊 الحالة / Status:* ");
        sb.append(order.getStatus()).append("\n");
        sb.append("*⚠️ الأولوية / Priority:* ");
        sb.append(order.getPriority()).append("\n");

        if (order.getExpectedDeliveryDate() != null) {
            sb.append("*📦 تاريخ التسليم المتوقع / Expected Delivery:* ");
            sb.append(order.getExpectedDeliveryDate()).append("\n");
        }

        if (order.getPaymentTerms() != null && !order.getPaymentTerms().isBlank()) {
            sb.append("*💳 شروط الدفع / Payment Terms:* ");
            sb.append(order.getPaymentTerms()).append("\n");
        }

        sb.append("\n");

        // Divider
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Items header
        sb.append("*📦 المنتجات المطلوبة / Order Items*\n\n");

        // Items list
        int index = 1;
        for (PurchaseOrderItem item : order.getItems()) {
            String productName = item.getProduct() != null ? item.getProduct().getName() : "Product unavailable";

            sb.append(index).append(". *").append(productName).append("*\n");
            sb.append("   _الكمية (Qty):_ ").append(item.getQuantity()).append("\n");
            sb.append("   _السعر (Price):_ 💰 ").append(item.getUnitPrice()).append(" EGP\n");
            sb.append("   _الإجمالي (Total):_ 💵 ").append(item.getTotalPrice()).append(" EGP\n");

            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                sb.append("   _ملاحظات (Notes):_ ").append(item.getNotes()).append("\n");
            }

            index++;
        }

        sb.append("\n");
        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n\n");

        // Total
        sb.append("*💵 إجمالي الطلب / Total Amount:* *").append(order.getTotalAmount()).append(" EGP*\n\n");

        // Footer
        sb.append("✅ *يرجى تأكيد الطلب في أقرب وقت*\n");
        sb.append("📞 *للتواصل مع المورد (Contact Supplier):* ");
        if (order.getSupplier().getPhone() != null) {
            sb.append(order.getSupplier().getPhone());
        }
        if (order.getSupplier().getEmail() != null) {
            sb.append(" | ").append(order.getSupplier().getEmail());
        }
        sb.append("\n\n");
        sb.append("_SmartPharma - إدارة الصيدليات الذكية_");

        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public SendEmailResponse sendPurchaseOrderEmail(Long orderId, Long pharmacyId) {
        log.info("Sending purchase order email for orderId: {}, pharmacyId: {}", orderId, pharmacyId);

        PurchaseOrder order = orderRepository.findByIdAndPharmacyIdAndDeletedAtIsNull(orderId, pharmacyId)
                .orElseThrow(() -> new RuntimeException("Purchase order not found with id: " + orderId));

        Supplier supplier = order.getSupplier();
        if (supplier == null) {
            throw new RuntimeException("No supplier assigned to this purchase order");
        }

        String email = supplier.getEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Supplier has no email address on file");
        }

        String subject = "Purchase Order " + order.getOrderNumber()
                + (order.getPharmacy() != null ? " - " + order.getPharmacy().getName() : "");
        String body = formatPurchaseOrderEmailShortBody(order);
        byte[] pdf = purchaseOrderPdfService.generatePdf(order);
        String filename = order.getOrderNumber() + ".pdf";

        emailService.sendEmailWithAttachment(email, subject, body, pdf, filename, "application/pdf");

        log.info("Purchase order email sent for order: {} -> {}", order.getOrderNumber(), email);

        return SendEmailResponse.builder()
                .success(true)
                .recipientEmail(email)
                .message("Email sent successfully")
                .build();
    }

    // The detailed order content now lives in the attached PDF (PurchaseOrderPdfService) -
    // the email body is just a short cover note pointing at it, not a second copy of
    // every line item in plain text.
    private String formatPurchaseOrderEmailShortBody(PurchaseOrder order) {
        Pharmacy pharmacy = order.getPharmacy();
        StringBuilder sb = new StringBuilder();

        sb.append("Dear ").append(order.getSupplier().getName()).append(",\n\n");
        sb.append("Please find attached Purchase Order ").append(order.getOrderNumber())
                .append(" (total: ").append(String.format("%.2f", order.getTotalAmount())).append(" EGP).\n\n");
        sb.append("Please confirm this order at your earliest convenience.\n\n");

        if (pharmacy != null) {
            sb.append("Best regards,\n");
            sb.append(pharmacy.getName() != null ? pharmacy.getName() : "SmartPharma");
            if (pharmacy.getPhone() != null && !pharmacy.getPhone().isBlank()) {
                sb.append("\n").append(pharmacy.getPhone());
            }
        } else {
            sb.append("Best regards,\nSmartPharma");
        }

        return sb.toString();
    }
}