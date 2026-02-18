package com.smartpharma.controller;

import com.smartpharma.dto.request.StockBatchRequest;
import com.smartpharma.dto.response.StockBatchResponse;
import com.smartpharma.service.StockBatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockBatchService stockBatchService;

    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<List<StockBatchResponse>> getAllBatches(@RequestParam Long pharmacyId) {
        return ResponseEntity.ok(stockBatchService.getAllBatches(pharmacyId));
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<StockBatchResponse> getBatch(@PathVariable Long id, @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(stockBatchService.getBatch(id, pharmacyId));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<StockBatchResponse> createBatch(@Valid @RequestBody StockBatchRequest request,
                                                          @RequestParam Long pharmacyId) {
        return ResponseEntity.ok(stockBatchService.createBatch(request, pharmacyId));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<List<StockBatchResponse>> getExpiringBatches(@RequestParam Long pharmacyId,
                                                                       @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(stockBatchService.getExpiringBatches(pharmacyId, days));
    }
}