package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierReorderGroupDTO {
    private Long supplierId;
    private String supplierName;
    private List<ReorderRecommendationDTO> recommendations;
}
