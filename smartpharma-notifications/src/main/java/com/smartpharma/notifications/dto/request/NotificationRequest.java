package com.smartpharma.notifications.dto.request;

import com.smartpharma.notifications.entity.Notification;
import com.smartpharma.common.entity.Pharmacy;
import com.smartpharma.common.entity.User;
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
    private String titleEn;
    private String messageEn;
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