package com.smartpharma.catalog.event;

import com.smartpharma.catalog.dto.response.StockBatchResponse;

public record StockChangedEvent(Long pharmacyId, String changeType, StockBatchResponse batch) {
}
