package com.aiincidentcommander.notification_service.event;

import com.aiincidentcommander.notification_service.service.WhatsAppNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private WhatsAppNotificationService whatsAppService;

    @InjectMocks
    private NotificationEventListener eventListener;

    @Test
    @DisplayName("Should parse event and call WhatsApp send on action.proposed")
    void testOnActionProposed() {
        Map<String, Object> payload = Map.of(
                "serviceName", "payment-service",
                "actionType", "RESTART_PODS"
        );

        IncidentEvent event = IncidentEvent.builder()
                .incidentId(10L)
                .sequenceNumber(1L)
                .timestamp(LocalDateTime.now())
                .eventType("action.proposed")
                .payload(payload)
                .build();

        eventListener.onActionProposed(event);

        verify(whatsAppService).send(
                eq(10L),
                eq("action.proposed"),
                contains("RESTART_PODS")
        );
    }

    @Test
    @DisplayName("Should parse event and call WhatsApp send on action.rolled_back")
    void testOnRollbackTriggered() {
        IncidentEvent event = IncidentEvent.builder()
                .incidentId(20L)
                .sequenceNumber(5L)
                .timestamp(LocalDateTime.now())
                .eventType("action.rolled_back")
                .payload(Map.of())
                .build();

        eventListener.onRollbackTriggered(event);

        verify(whatsAppService).send(
                eq(20L),
                eq("action.rolled_back"),
                contains("Rollback in progress")
        );
    }

    @Test
    @DisplayName("Should parse event and call WhatsApp send on incident.escalated")
    void testOnIncidentEscalated() {
        Map<String, Object> payload = Map.of("serviceName", "auth-service");

        IncidentEvent event = IncidentEvent.builder()
                .incidentId(30L)
                .sequenceNumber(8L)
                .timestamp(LocalDateTime.now())
                .eventType("incident.escalated")
                .payload(payload)
                .build();

        eventListener.onIncidentEscalated(event);

        verify(whatsAppService).send(
                eq(30L),
                eq("incident.escalated"),
                contains("auth-service")
        );
    }
}
