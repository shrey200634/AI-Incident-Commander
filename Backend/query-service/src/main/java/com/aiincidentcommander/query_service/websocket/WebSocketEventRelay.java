package com.aiincidentcommander.query_service.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventRelay {

    private final SimpMessagingTemplate messagingTemplate;

    public void pushIncidentUpdate(Long incidentId, Object payload) {
        String destination = "/topic/incidents/" + incidentId;
        messagingTemplate.convertAndSend(destination, payload);
        log.debug("Pushed update to {}", destination);
    }

    public void pushActiveIncidentsUpdate(Object payload) {
        messagingTemplate.convertAndSend("/topic/incidents/active", payload);
    }
}