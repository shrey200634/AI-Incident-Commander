package com.aiincidentcommander.command_service.service;

import com.aiincidentcommander.command_service.dto.ActionProposed;
import com.aiincidentcommander.command_service.dto.CreateIncident;
import com.aiincidentcommander.command_service.dto.IncidentResponse;
import com.aiincidentcommander.command_service.dto.RemediationActionResponse;
import com.aiincidentcommander.command_service.event.IncidentEvent;
import com.aiincidentcommander.command_service.event.KafkaEventPublisher;
import com.aiincidentcommander.command_service.exception.IncidentNotFoundException;
import com.aiincidentcommander.command_service.model.ActionStatus;
import com.aiincidentcommander.command_service.model.Incident;
import com.aiincidentcommander.command_service.model.RemediationAction;
import com.aiincidentcommander.command_service.repository.IncidentRep;
import com.aiincidentcommander.command_service.repository.RemediationActionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentService {
    private static final String TOPIC_INCIDENT_CREATED = "incident.created";
    private static final String TOPIC_ACTION_PROPOSED = "action_proposed";

    private final IncidentRep incidentRep ;
    private final RemediationActionRepository actionRepository ;
    private final KafkaEventPublisher kafkaEventPublisher ;

    @Transactional
    public IncidentResponse createIncident(CreateIncident request){
        Incident incident = Incident.builder()
                .serviceName(request.getServiceName())
                .severity(request.getSeverity())
                .build();

        Incident saved = incidentRep.save(incident);
        log.info("Incident created: id={}, service={}, severity={}",
                saved.getId(), saved.getServiceName(), saved.getSeverity());
        publishEvent(TOPIC_INCIDENT_CREATED , saved.getId() , toResponse(saved));

        return toResponse(saved);

    }

    @Transactional
    public RemediationActionResponse proposeResponse(Long id , ActionProposed request){
        Incident incident  = incidentRep.findById(id)
                .orElseThrow(()-> new IncidentNotFoundException(id));

        RemediationAction action = RemediationAction.builder()
                .incidentId(incident.getId())
                .actionType(request.getActionType())
                .rationale(request.getRational())
                .status(ActionStatus.PROPOSED)
                .build();

        RemediationAction saved = actionRepository.save(action);
        log.info("Action proposed: incidentId={}, actionId={}, type={}",
                id, saved.getId(), saved.getActionType());
        publishEvent(TOPIC_ACTION_PROPOSED , id , toResponseRemediation(saved));
        return toResponseRemediation(saved);

    }

    private void publishEvent(String topic , Long id , Object payload ){
        IncidentEvent event = IncidentEvent.builder()
                .incidentId(id)
                .sequenceNumber(1L)
                .timestamp( LocalDateTime.now())
                .eventType(topic)
                .payload(payload)
                .build();

        kafkaEventPublisher.publish(topic, id , event);
    }

    private IncidentResponse toResponse(Incident incident){
        return IncidentResponse.builder()
                .id(incident.getId())
                .serviceName(incident.getServiceName())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .build();
    }

    private RemediationActionResponse toResponseRemediation(RemediationAction action){
        return RemediationActionResponse.builder()
                .id(action.getId())
                .incidentId(action.getIncidentId())
                .actionType(action.getActionType())
                .rational(action.getRationale())
                .status(action.getStatus())
                .createdAt(action.getCreatedAt())
                .build();
    }
}
