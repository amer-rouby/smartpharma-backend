package com.smartpharma.sales.service.impl;

import com.smartpharma.catalog.entity.StockBatch;
import com.smartpharma.catalog.repository.StockBatchRepository;
import com.smartpharma.common.entity.User;
import com.smartpharma.common.exception.LocalizedException;
import com.smartpharma.common.exception.ResourceNotFoundException;
import com.smartpharma.common.repository.UserRepository;
import com.smartpharma.sales.dto.request.SaleReturnRequest;
import com.smartpharma.sales.dto.response.SaleReturnDTO;
import com.smartpharma.sales.entity.SaleItem;
import com.smartpharma.sales.entity.SaleReturn;
import com.smartpharma.sales.entity.SaleReturnItem;
import com.smartpharma.sales.entity.SaleTransaction;
import com.smartpharma.sales.event.SaleReturnedEvent;
import com.smartpharma.sales.repository.SaleReturnRepository;
import com.smartpharma.sales.repository.SaleTransactionRepository;
import com.smartpharma.sales.service.SaleReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Returns of part (or all) of a sale. The sale stays as sold; each return
// records what came back, puts it back in stock (unless it can't be resold)
// and refunds its share of the sale. The ETA return receipt is issued after
// commit by the e-invoice module (SaleReturnedEvent).
@Service
@RequiredArgsConstructor
@Slf4j
public class SaleReturnServiceImpl implements SaleReturnService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final SaleReturnRepository saleReturnRepository;
    private final StockBatchRepository stockBatchRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SaleReturnDTO createReturn(Long saleId, SaleReturnRequest request, Long pharmacyId, Long userId) {
        SaleTransaction sale = saleTransactionRepository.lockByIdAndPharmacyId(saleId, pharmacyId)
                .orElseThrow(() -> new ResourceNotFoundException("SALE_NOT_FOUND", "Sale not found: " + saleId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found: " + userId));

        Map<Long, Integer> alreadyReturned = returnedQuantities(saleId);
        Map<Long, Integer> requested = new LinkedHashMap<>();
        for (SaleReturnRequest.Item item : request.getItems()) {
            requested.merge(item.getSaleItemId(), item.getQuantity(), Integer::sum);
        }

        List<SaleReturn> previous = saleReturnRepository.findBySale(saleId, pharmacyId);
        SaleReturn saleReturn = SaleReturn.builder()
                .saleTransaction(sale)
                .pharmacy(sale.getPharmacy())
                .user(user)
                .returnNumber(previous.size() + 1)
                .restocked(request.isRestock())
                .reason(blankToNull(request.getReason()))
                .build();

        BigDecimal itemsTotal = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : requested.entrySet()) {
            SaleItem saleItem = sale.getItems().stream()
                    .filter(i -> i.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new LocalizedException(HttpStatus.BAD_REQUEST, "SALE_RETURN_ITEM_NOT_IN_SALE",
                            "Item " + entry.getKey() + " isn't part of sale " + saleId));
            int quantity = entry.getValue();
            int available = saleItem.getQuantity() - alreadyReturned.getOrDefault(saleItem.getId(), 0);
            if (quantity > available) {
                throw new LocalizedException(HttpStatus.BAD_REQUEST, "SALE_RETURN_QUANTITY_EXCEEDED",
                        "Only " + available + " of " + saleItem.getProduct().getName() + " can still be returned",
                        Map.of("productName", saleItem.getProduct().getName(), "available", available));
            }

            BigDecimal lineTotal = saleItem.getUnitPrice().multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            saleReturn.getItems().add(SaleReturnItem.builder()
                    .saleReturn(saleReturn)
                    .saleItem(saleItem)
                    .quantity(quantity)
                    .unitPrice(saleItem.getUnitPrice())
                    .totalPrice(lineTotal)
                    .build());
            itemsTotal = itemsTotal.add(lineTotal);

            if (request.isRestock()) {
                restock(saleItem, quantity);
            }
        }

        boolean everythingReturned = sale.getItems().stream().allMatch(i ->
                i.getQuantity() == alreadyReturned.getOrDefault(i.getId(), 0) + requested.getOrDefault(i.getId(), 0));
        BigDecimal previousShares = previous.stream()
                .map(SaleReturn::getDiscountShare)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discountShare = discountShare(sale.getDiscountAmount(), sale.getSubtotal(), itemsTotal,
                previousShares, everythingReturned);

        saleReturn.setItemsTotal(itemsTotal);
        saleReturn.setDiscountShare(discountShare);
        saleReturn.setRefundAmount(itemsTotal.subtract(discountShare));
        SaleReturn saved = saleReturnRepository.save(saleReturn);

        BigDecimal returnedSoFar = Optional.ofNullable(sale.getReturnedAmount()).orElse(BigDecimal.ZERO);
        sale.setReturnedAmount(returnedSoFar.add(saved.getRefundAmount()));
        saleTransactionRepository.save(sale);

        log.info("Sale return {} recorded | sale {} | refund {} | restocked {}",
                saved.getReturnNumber(), saleId, saved.getRefundAmount(), saved.isRestocked());
        eventPublisher.publishEvent(new SaleReturnedEvent(pharmacyId, saleId, saved.getId()));
        return SaleReturnDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleReturnDTO> getReturns(Long saleId, Long pharmacyId) {
        return saleReturnRepository.findBySale(saleId, pharmacyId).stream()
                .map(SaleReturnDTO::fromEntity)
                .toList();
    }

    /**
     * The returned items' share of the sale-level discount, in proportion to
     * their shelf value. The return that brings back the last items gets
     * whatever is left, so the shares always add up to the sale's discount.
     */
    static BigDecimal discountShare(BigDecimal saleDiscount, BigDecimal saleSubtotal, BigDecimal itemsTotal,
                                    BigDecimal previousShares, boolean everythingReturned) {
        BigDecimal discount = saleDiscount == null ? BigDecimal.ZERO : saleDiscount;
        if (discount.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        if (everythingReturned) {
            return discount.subtract(previousShares).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        }
        if (saleSubtotal == null || saleSubtotal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return discount.multiply(itemsTotal).divide(saleSubtotal, 2, RoundingMode.HALF_UP);
    }

    Map<Long, Integer> returnedQuantities(Long saleId) {
        Map<Long, Integer> returned = new HashMap<>();
        for (Object[] row : saleReturnRepository.sumReturnedQuantities(saleId)) {
            returned.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return returned;
    }

    // Back into the batch it was sold from, so expiry tracking stays right. An
    // expired or discarded batch keeps its status: the units are counted but
    // not offered for sale again.
    private void restock(SaleItem saleItem, int quantity) {
        StockBatch batch = saleItem.getBatch();
        if (batch == null) {
            log.warn("Sale item {} has no batch - returned stock not added back", saleItem.getId());
            return;
        }
        batch.setQuantityCurrent(Optional.ofNullable(batch.getQuantityCurrent()).orElse(0) + quantity);
        stockBatchRepository.save(batch);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
