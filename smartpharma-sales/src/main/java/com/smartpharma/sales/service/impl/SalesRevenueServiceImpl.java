package com.smartpharma.sales.service.impl;

import com.smartpharma.sales.repository.SaleTransactionRepository;
import com.smartpharma.sales.service.SalesRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesRevenueServiceImpl implements SalesRevenueService {

    private final SaleTransactionRepository saleTransactionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getDailySales(Long pharmacyId, LocalDateTime startDate, LocalDateTime endDate) {
        return DailyNet.merge(saleTransactionRepository.getDailySalesOnly(pharmacyId, startDate, endDate),
                saleTransactionRepository.getDailyRefunds(pharmacyId, startDate, endDate), true);
    }
}
