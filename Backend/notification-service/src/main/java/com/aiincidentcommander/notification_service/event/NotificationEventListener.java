package com.aiincidentcommander.notification_service.event;

import com.aiincidentcommander.notification_service.service.WhatsAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationEventListener {

    private final WhatsAppNotificationService whatsAppService;

    @KafkaListener(topics = "action.proposed", groupId = "notification-service-group")
    public void onActionProposed(IncidentEvent event) {
        log.info("Notification received action.proposed: incidentId={}", event.getIncidentId());

        String serviceName = extractField(event, "serviceName", "unknown-service");
        String actionType = extractField(event, "actionType", "unknown-action");

        String message = String.format(
                "🚨 Incident #%d on %s: AI agent proposes *%s*. Reply/approve in dashboard.",
                event.getIncidentId(), serviceName, actionType
        );

        whatsAppService.send(event.getIncidentId(), "action.proposed", message);
    }

    @KafkaListener(topics = "action.rolled_back", groupId = "notification-service-group")
    public void onRollbackTriggered(IncidentEvent event) {
        log.info("Notification received action.rolled_back: incidentId={}", event.getIncidentId());

        String message = String.format(
                "⚠️ Incident #%d: Remediation did not resolve the issue. Rollback in progress.",
                event.getIncidentId()
        );

        whatsAppService.send(event.getIncidentId(), "action.rolled_back", message);
    }

    @KafkaListener(topics = "incident.escalated", groupId = "notification-service-group")
    public void onIncidentEscalated(IncidentEvent event) {
        log.info("Notification received incident.escalated: incidentId={}", event.getIncidentId());

        String serviceName = extractField(event, "serviceName", "unknown-service");

        String message = String.format(
                "🔴 Incident #%d on %s: Rollback FAILED. Manual intervention required immediately!",
                event.getIncidentId(), serviceName
        );

        whatsAppService.send(event.getIncidentId(), "incident.escalated", message);
    }

    @SuppressWarnings("unchecked")
    private String extractField(IncidentEvent event, String field, String fallback) {
        if (event.getPayload() instanceof Map<?, ?> payloadMap) {
            Object value = payloadMap.get(field);
            if (value != null) return value.toString();
        }
        return fallback;
    }
}
