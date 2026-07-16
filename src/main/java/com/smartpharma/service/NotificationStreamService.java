package com.smartpharma.service;

import com.smartpharma.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class NotificationStreamService {

    private static final long EMITTER_TIMEOUT_MS = 30L * 60L * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByPharmacy = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long pharmacyId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emittersByPharmacy.computeIfAbsent(pharmacyId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(pharmacyId, emitter));
        emitter.onTimeout(() -> removeEmitter(pharmacyId, emitter));
        emitter.onError(error -> removeEmitter(pharmacyId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ex) {
            removeEmitter(pharmacyId, emitter);
        }

        return emitter;
    }

    public void notifyCreated(Long pharmacyId, NotificationResponse notification) {
        if (pharmacyId == null || notification == null) {
            return;
        }
        sendEvent(pharmacyId, "notification-created", notification);
    }

    public void notifyChanged(Long pharmacyId) {
        if (pharmacyId == null) {
            return;
        }
        sendEvent(pharmacyId, "notifications-changed", Map.of("refresh", true));
    }

    private void sendEvent(Long pharmacyId, String eventName, Object data) {
        List<SseEmitter> emitters = emittersByPharmacy.get(pharmacyId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException | IllegalStateException ex) {
                removeEmitter(pharmacyId, emitter);
            }
        }
    }

    private void removeEmitter(Long pharmacyId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByPharmacy.get(pharmacyId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByPharmacy.remove(pharmacyId);
        }
    }
}
