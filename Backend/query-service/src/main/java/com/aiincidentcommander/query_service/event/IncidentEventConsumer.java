package com.aiincidentcommander.query_service.event;

import com.aiincidentcommander.query_service.model.IncidentReadModel;
import com.aiincidentcommander.query_service.model.IncidentStatus;
import com.aiincidentcommander.query_service.repo.IncidentReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentEventConsumer {

    private final IncidentReadRepository incidentReadRepository;
    private final ObjectMapper objectMapper;

    // incident event
    @KafkaListener(topics = "incident.created", groupId = "query-service-group")
    public void onIncidentCreated(IncidentEvent event) {
        log.info("Received incident.created: incidentId={}", event.getIncidentId());
        Map<String , Object> payload = toMap(event.getPayload());

        IncidentReadModel model = IncidentReadModel.builder()
                .id(toLong(payload.get("id")))
                .serviceName((String) payload.get("serviceName"))
                .severity((String) payload.get("severity"))
                .status(IncidentStatus.valueOf((String) payload.get("status")))
                .createdAt(toLocalDateTime(payload.get("createdAt")))
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        incidentReadRepository.save(model);
        log.info("Saved incident read model: id={}", model.getId());
    }




    @KafkaListener(topics = "incident.escalated", groupId = "query-service-group")
    public void onIncidentEscalated(IncidentEvent event) {
        log.info("Received incident.escalated: incidentId={}", event.getIncidentId());
        Map<String , Object> payload = toMap(event.getPayload());

        incidentReadRepository.findById(event.getIncidentId()).ifPresentOrElse(model -> {
            model.setStatus(IncidentStatus.ESCALATED);
            model.setEscalationReason((String) payload.get("escalationReason"));
            model.setLastUpdatedAt(LocalDateTime.now());
            incidentReadRepository.save(model);
            log.info("Updated incident to ESCALATED: id={}", model.getId());

        }, () -> log.warn("Incident not found for escalation: id={}", event.getIncidentId()));
    }



    @KafkaListener(topics = "incident.resolved", groupId = "query-service-group")
    public void onIncidentResolved(IncidentEvent event) {
        log.info("Received incident.resolved: incidentId={}", event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());
        incidentReadRepository.findById(event.getIncidentId()).ifPresentOrElse(model -> {
            model.setStatus(IncidentStatus.RESOLVED);
            model.setResolvedAt(toLocalDateTime(payload.get("resolvedAt")));
            model.setLastUpdatedAt(LocalDateTime.now());
            incidentReadRepository.save(model);
            log.info("Updated incident to RESOLVED: id={}", model.getId());
        }, () -> log.warn("Incident not found for resolution: id={}", event.getIncidentId()));
    }

    @KafkaListener(topics = "incident.status_updated", groupId = "query-service-group")
    public void onIncidentStatusUpdated(IncidentEvent event) {
        log.info("Received incident.status_updated: incidentId={}", event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());

        incidentReadRepository.findById(event.getIncidentId()).ifPresentOrElse(model -> {
            model.setStatus(IncidentStatus.valueOf((String) payload.get("status")));
            model.setLastUpdatedAt(LocalDateTime.now());
            incidentReadRepository.save(model);
            log.info("Updated incident status to {}: id={}", model.getStatus(), model.getId());
        }, () -> log.warn("Incident not found for status update: id={}", event.getIncidentId()));
    }

    //helper ----------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String,Object> toMap (Object payload ){
        return (Map<String,Object>) payload;
    }

    private Long toLong(Object value ){
        if (value==null) return  null ;
        return  Long.valueOf(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        return objectMapper.convertValue(value, LocalDateTime.class);
    }
}
