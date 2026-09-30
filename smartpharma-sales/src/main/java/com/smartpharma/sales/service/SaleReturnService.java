package com.smartpharma.sales.service;

import com.smartpharma.sales.dto.request.SaleReturnRequest;
import com.smartpharma.sales.dto.response.SaleReturnDTO;

import java.util.List;

public interface SaleReturnService {

    SaleReturnDTO createReturn(Long saleId, SaleReturnRequest request, Long pharmacyId, Long userId);

    List<SaleReturnDTO> getReturns(Long saleId, Long pharmacyId);
}
