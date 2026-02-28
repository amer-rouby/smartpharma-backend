package com.smartpharma.dto.request;

import com.smartpharma.entity.Notification;
import com.smartpharma.entity.Pharmacy;
import com.smartpharma.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    private Pharmacy pharmacy;
    private User recipient;
    private String title;
    private String message;
    private Notification.NotificationType type;
    private Notification.NotificationPriority priority;
    private String relatedEntityType;
    private Long relatedEntityId;

    public boolean isValid() {
        return pharmacy != null
                && title != null && !title.isBlank()
                && message != null && !message.isBlank()
                && type != null;
    }
}