package com.aiincidentcommander.command_service.service;

import com.aiincidentcommander.command_service.config.KafkaTopicConfig;
import com.aiincidentcommander.command_service.dto.*;
import com.aiincidentcommander.command_service.event.IncidentEvent;
import com.aiincidentcommander.command_service.event.KafkaEventPublisher;
import com.aiincidentcommander.command_service.exception.ActionNotFoundException;
import com.aiincidentcommander.command_service.exception.IncidentNotFoundException;
import com.aiincidentcommander.command_service.exception.InvalidStateTransitionException;
import com.aiincidentcommander.command_service.model.ActionStatus;
import com.aiincidentcommander.command_service.model.Incident;
import com.aiincidentcommander.command_service.model.IncidentStatus;
import com.aiincidentcommander.command_service.model.RemediationAction;
import com.aiincidentcommander.command_service.repository.IncidentRep;
import com.aiincidentcommander.command_service.repository.RemediationActionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static com.aiincidentcommander.command_service.config.KafkaTopicConfig.TOPIC_ACTION_PROPOSED;
import static com.aiincidentcommander.command_service.config.KafkaTopicConfig.TOPIC_INCIDENT_CREATED;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRep incidentRep ;
    private final RemediationActionRepository actionRepository ;
    private final KafkaEventPublisher kafkaEventPublisher ;

    private   final AtomicLong sequenceCounter = new AtomicLong(0);

    // valid state transition

    private static final Map<IncidentStatus , Set<IncidentStatus>> VALID_TRANSITION = Map.of(

            IncidentStatus.NEW, Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.ESCALATED),
            IncidentStatus.INVESTIGATING, Set.of(IncidentStatus.ACTION_PROPOSED, IncidentStatus.ESCALATED),
            IncidentStatus.ACTION_PROPOSED, Set.of(IncidentStatus.WAITING_APPROVAL, IncidentStatus.ESCALATED),
            IncidentStatus.WAITING_APPROVAL, Set.of(IncidentStatus.EXECUTING, IncidentStatus.ESCALATED),
            IncidentStatus.EXECUTING, Set.of(IncidentStatus.MONITORING, IncidentStatus.ROLLBACK, IncidentStatus.ESCALATED),
            IncidentStatus.MONITORING, Set.of(IncidentStatus.RESOLVED, IncidentStatus.ROLLBACK, IncidentStatus.ESCALATED),
            IncidentStatus.ROLLBACK, Set.of(IncidentStatus.INVESTIGATING, IncidentStatus.ESCALATED),
            IncidentStatus.RESOLVED, Set.of(),
            IncidentStatus.ESCALATED, Set.of()
    );


    // create incident
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
       // propose action
    @Transactional
    public RemediationActionResponse proposeResponse(Long id , ActionProposed request){
        Incident incident  = incidentRep.findById(id)
                .orElseThrow(()-> new IncidentNotFoundException(id));

        RemediationAction action = RemediationAction.builder()
                .incidentId(incident.getId())
                .actionType(request.getActionType())
                .rationale(request.getRationale())
                .status(ActionStatus.PROPOSED)
                .build();

        RemediationAction saved = actionRepository.save(action);
        log.info("Action proposed: incidentId={}, actionId={}, type={}",
                id, saved.getId(), saved.getActionType());
        publishEvent(TOPIC_ACTION_PROPOSED , id , toResponseRemediation(saved));
        return toResponseRemediation(saved);

    }
    //approve action

    @Transactional
    public RemediationActionResponse approveAction(Long id , Long actionId , ApproveActionRequest request){
        Incident incident =findIncidentOrThrow(id);
        RemediationAction action = findActionOrThrow(actionId);

        if (action.getStatus() != ActionStatus.PROPOSED){
            throw new InvalidStateTransitionException(action.getStatus().name() , ActionStatus.APPROVED.name());
        }
        action.setStatus(ActionStatus.APPROVED);
        action.setApprovedBy(request.getApprovedBy());
        actionRepository.save(action);

        transitionStatus(incident , IncidentStatus.WAITING_APPROVAL);
        log.info("Action approved: incidentId={}, actionId={}, approvedBy={}",
                id, actionId, request.getApprovedBy());

        publishEvent(KafkaTopicConfig.TOPIC_ACTION_APPROVED , id , toResponseRemediation(action));
        return toResponseRemediation(action);
    }

    //execute Action

    @Transactional
    public  RemediationActionResponse executeAction ( Long id , long actionId ){
        Incident incident = findIncidentOrThrow(id);
        RemediationAction action = findActionOrThrow(actionId);

        if (action.getStatus()!= ActionStatus.APPROVED){
            throw new InvalidStateTransitionException(action.getStatus().name(), ActionStatus.EXECUTED.name());
        }
        action.setStatus(ActionStatus.EXECUTED);
        action.setExecutedAt(LocalDateTime.now() );
        actionRepository.save(action);

        transitionStatus(incident , IncidentStatus.EXECUTING);
        log.info("Action executed: incidentId={}, actionId={}", id, actionId);

        publishEvent(KafkaTopicConfig.TOPIC_ACTION_EXECUTED , id , toResponseRemediation(action));
        return toResponseRemediation(action);

    }

    //roll-back
    @Transactional
    public  RemediationActionResponse rollBackAction(Long id , Long actionId ){
        Incident incident = findIncidentOrThrow( id );
        RemediationAction action = findActionOrThrow(actionId );

        if (action.getStatus() != ActionStatus.EXECUTED){
            throw new InvalidStateTransitionException(action.getStatus().name() , ActionStatus.ROLLED_BACK.name());
        }
        action.setStatus(ActionStatus.ROLLED_BACK);
        actionRepository.save(action );

        RemediationAction rollbackAction = RemediationAction.builder()
                .incidentId(id)
                .actionType("ROLLBACK_" + action.getActionType())
                .rationale("Rollback of action " + actionId)
                .status(ActionStatus.EXECUTED)
                .executedAt(LocalDateTime.now())
                .rollbackOf(actionId)
                .build();
        actionRepository.save(rollbackAction);

        transitionStatus(incident , IncidentStatus.ROLLBACK);
        log.info("Action rolled back: incidentId={}, actionId={}", id, actionId);

        publishEvent(KafkaTopicConfig.TOPIC_ACTION_ROLLED_BACK , id , toResponseRemediation(action));
        return  toResponseRemediation( action);

    }

    //update transition status (escalate/ resolved)
    @Transactional
    public  IncidentResponse updateIncidentStatus ( Long id , UpdateStatusRequest request ) {
        Incident incident = findIncidentOrThrow(id );

        IncidentStatus targetStatus ;
        try{
            targetStatus = IncidentStatus.valueOf(request.getTargetStatus().toUpperCase());
        }catch (IllegalArgumentException ex ){
            throw new InvalidStateTransitionException(incident.getStatus().name() , request.getTargetStatus());
        }

        transitionStatus(incident , targetStatus);

        if ( targetStatus  == IncidentStatus.ESCALATED){
            incident.setEscalationReason(request.getReason());
            incidentRep.save(incident);
            log.info("Incident escalated: id={}, reason={}", id, request.getReason());
            publishEvent(KafkaTopicConfig.TOPIC_INCIDENT_ESCALATED, id , toResponse(incident));

        }
        if (targetStatus == IncidentStatus.RESOLVED){
            incident.setResolvedAt(LocalDateTime.now());
            incidentRep.save(incident);
            log.info("Incident resolved: id={}", id);
            publishEvent(KafkaTopicConfig.TOPIC_INCIDENT_RESOLVED, id , toResponse(incident));
        }
        return  toResponse( incident);
    }

    // get Incident

     public  IncidentResponse getIncident(Long id ){
        Incident incident = findIncidentOrThrow(id);
        return toResponse(incident);
     }
    //-------------------------------------------------------------------------------------------------------------

    private void publishEvent(String topic , Long id , Object payload ){
        IncidentEvent event = IncidentEvent.builder()
                .incidentId(id)
                .sequenceNumber(sequenceCounter.incrementAndGet())
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
                .rationale(action.getRationale())
                .status(action.getStatus())
                .createdAt(action.getCreatedAt())
                .build();
    }

    private   Incident findIncidentOrThrow(Long id ){
        return  incidentRep.findById(id )
                .orElseThrow(()-> new IncidentNotFoundException(id ));
    }


    private   RemediationAction findActionOrThrow(Long id ){
        return actionRepository.findById(id)
                .orElseThrow(()-> new ActionNotFoundException(id));
    }


    private void transitionStatus(Incident incident, IncidentStatus target) {
        IncidentStatus current = incident.getStatus();
        Set<IncidentStatus> allowed = VALID_TRANSITION.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStateTransitionException(current.name(), target.name());
        }
        incident.setStatus(target);
        incidentRep.save(incident);
    }

}
