package com.smartpharma.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;
    private String title;
    private String message;
    private String type;
    private String priority;
    private boolean read;
    private String createdAt;
    private String relatedEntityType;
    private Long relatedEntityId;
    private String typeLabelAr;
    private String priorityLabelAr;
    public String getIconName() {
        if (type == null) return "notifications";

        return switch (type) {
            case "LOW_STOCK" -> "inventory_2";
            case "EXPIRY_WARNING" -> "warning";
            case "EXPIRED" -> "error";
            case "SALE_COMPLETED" -> "check_circle";
            case "EXPENSE_ADDED" -> "receipt_long";
            case "SYSTEM" -> "info";
            default -> "notifications";
        };
    }

    public String getPriorityColor() {
        if (priority == null) return "#6b7280";

        return switch (priority) {
            case "URGENT" -> "#dc2626";
            case "HIGH" -> "#f59e0b";
            case "MEDIUM" -> "#3b82f6";
            case "LOW" -> "#6b7280";
            default -> "#6b7280";
        };
    }
}