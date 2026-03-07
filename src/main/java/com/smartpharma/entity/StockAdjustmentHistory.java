package com.smartpharma.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_adjustment_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustmentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private StockBatch batch;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String reason;

    @Column(name = "previous_quantity", nullable = false)
    private Integer previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private Integer newQuantity;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "adjustment_date", nullable = false)
    private LocalDateTime adjustmentDate;

    @Column(name = "adjusted_by")
    private Long adjustedBy;

    @Column(name = "adjusted_by_name")
    private String adjustedByName;

    @PrePersist
    protected void onCreate() {
        adjustmentDate = LocalDateTime.now();
    }
}