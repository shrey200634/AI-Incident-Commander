package com.aiincidentcommander.query_service.event;


import com.aiincidentcommander.query_service.model.ActionReadModel;
import com.aiincidentcommander.query_service.model.ActionStatus;
import com.aiincidentcommander.query_service.model.IncidentStatus;
import com.aiincidentcommander.query_service.repo.ActionReadRepo;
import com.aiincidentcommander.query_service.repo.IncidentReadRepository;
import com.aiincidentcommander.query_service.service.IncidentQueryService;
import com.aiincidentcommander.query_service.websocket.WebSocketEventRelay;
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
public class ActionEventConsumer {

    private final ObjectMapper objectMapper;
    private final ActionReadRepo actionReadRepo;
    private final IncidentReadRepository incidentReadRepository;
    private final WebSocketEventRelay webSocketEventRelay;
    private final IncidentQueryService incidentQueryService;



    // action events
    @KafkaListener(topics = "action.proposed" , groupId = "query-service-group")
    public  void onActionProposed(IncidentEvent event){
        log.info("Received action proposed: incident id ={}" , event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());

        ActionReadModel model = ActionReadModel.builder()
                .lastSequenceNumber(event.getSequenceNumber())
                .id(toLong(payload.get("id")))
                .incidentId(toLong(payload.get("incidentId")))
                .actionType((String) payload.get("actionType"))
                .rationale((String) payload.get("rationale"))
                .status(ActionStatus.valueOf((String) payload.get("status")))
                .createdAt(toLocalDateTime(payload.get("createdAt")))
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        actionReadRepo.save(model);
        webSocketEventRelay.pushIncidentUpdate(event.getIncidentId(), event.getPayload());
        webSocketEventRelay.pushActiveIncidentsUpdate(event.getPayload());
        log.info("Saved action read model: id={}", model.getId());
    }


    @KafkaListener(topics = "action.rejected", groupId = "query-service-group")
    public void onActionRejected(IncidentEvent event) {
        log.info("Received action.rejected: incidentId={}", event.getIncidentId());

        Map<String, Object> payload = toMap(event.getPayload());
        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {
            if (!isNextInSequence(model.getLastSequenceNumber(), event.getSequenceNumber())) {
                log.warn("Discarding out-of-sequence action.rejected: incidentId={}, lastSeq={}, incomingSeq={}",
                        event.getIncidentId(), model.getLastSequenceNumber(), event.getSequenceNumber());
                return;
            }
            model.setLastSequenceNumber(event.getSequenceNumber());
            model.setStatus(ActionStatus.REJECTED);
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);
            log.info("Updated action to REJECTED: id={}", actionId);

            incidentReadRepository.findById(event.getIncidentId()).ifPresent(incident -> {
                incident.setStatus(IncidentStatus.INVESTIGATING);
                incident.setLastUpdatedAt(LocalDateTime.now());
                incidentReadRepository.save(incident);
                incidentQueryService.evictIncidentCache(incident.getId());
                webSocketEventRelay.pushIncidentUpdate(event.getIncidentId(), event.getPayload());
                webSocketEventRelay.pushActiveIncidentsUpdate(event.getPayload());
                log.info("Updated incident status to INVESTIGATING: id={}", incident.getId());
            });
        }, () -> log.warn("Action not found for rejection: id={}", actionId));
    }

    @KafkaListener(topics = "action.approved", groupId = "query-service-group")
    public void onActionApproved(IncidentEvent event) {
        log.info("Received action.approved: incidentId={}", event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());

        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {
            if (!isNextInSequence(model.getLastSequenceNumber(), event.getSequenceNumber())) {
                log.warn("Discarding out-of-sequence action.approved: incidentId={}, lastSeq={}, incomingSeq={}",
                        event.getIncidentId(), model.getLastSequenceNumber(), event.getSequenceNumber());
                return;
            }
            model.setLastSequenceNumber(event.getSequenceNumber());
            model.setStatus(ActionStatus.APPROVED);
            model.setApprovedBy((String) payload.get("approvedBy"));
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);

            log.info("Updated action to APPROVED: id={}", actionId);

            incidentReadRepository.findById(event.getIncidentId()).ifPresent(incident -> {
                incident.setStatus(IncidentStatus.WAITING_APPROVAL);
                incident.setLastUpdatedAt(LocalDateTime.now());
                incidentReadRepository.save(incident);
                incidentQueryService.evictIncidentCache(incident.getId());
                webSocketEventRelay.pushIncidentUpdate(event.getIncidentId(), event.getPayload());
                webSocketEventRelay.pushActiveIncidentsUpdate(event.getPayload());
                log.info("Updated incident status to WAITING_APPROVAL: id={}", incident.getId());
            });
        }, () -> log.warn("Action not found for approval: id={}", actionId));
    }

    @KafkaListener(topics = "action.executed", groupId = "query-service-group")
    public void onActionExecuted(IncidentEvent event) {
        log.info("Received action.executed: incidentId={}", event.getIncidentId());

        Map<String, Object> payload = toMap(event.getPayload());
        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {
            if (!isNextInSequence(model.getLastSequenceNumber(), event.getSequenceNumber())) {
                log.warn("Discarding out-of-sequence action.executed: incidentId={}, lastSeq={}, incomingSeq={}",
                        event.getIncidentId(), model.getLastSequenceNumber(), event.getSequenceNumber());
                return;
            }
            model.setLastSequenceNumber(event.getSequenceNumber());
            model.setStatus(ActionStatus.EXECUTED);
            model.setExecutedAt(toLocalDateTime(payload.get("executedAt")));
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);

            log.info("Updated action to EXECUTED: id={}", actionId);

            incidentReadRepository.findById(event.getIncidentId()).ifPresent(incident -> {
                incident.setStatus(IncidentStatus.EXECUTING);
                incident.setLastUpdatedAt(LocalDateTime.now());
                incidentReadRepository.save(incident);
                incidentQueryService.evictIncidentCache(incident.getId());
                webSocketEventRelay.pushIncidentUpdate(event.getIncidentId(), event.getPayload());
                webSocketEventRelay.pushActiveIncidentsUpdate(event.getPayload());
                log.info("Updated incident status to EXECUTING: id={}", incident.getId());
            });
        }, () -> log.warn("Action not found for execution: id={}", actionId));
    }

    @KafkaListener(topics = "action.rolled_back", groupId = "query-service-group")
    public void onActionRolledBack(IncidentEvent event) {
        log.info("Received action.rolled_back: incidentId={}", event.getIncidentId());

        Map<String, Object> payload = toMap(event.getPayload());
        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {
            if (!isNextInSequence(model.getLastSequenceNumber(), event.getSequenceNumber())) {
                log.warn("Discarding out-of-sequence action.rolled_back: incidentId={}, lastSeq={}, incomingSeq={}",
                        event.getIncidentId(), model.getLastSequenceNumber(), event.getSequenceNumber());
                return;
            }
            model.setLastSequenceNumber(event.getSequenceNumber());
            model.setStatus(ActionStatus.ROLLED_BACK);
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);
            log.info("Updated action to ROLLED_BACK: id={}", actionId);

            incidentReadRepository.findById(event.getIncidentId()).ifPresent(incident -> {
                incident.setStatus(IncidentStatus.ROLLBACK);
                incident.setLastUpdatedAt(LocalDateTime.now());
                incidentReadRepository.save(incident);
                incidentQueryService.evictIncidentCache(incident.getId());
                webSocketEventRelay.pushIncidentUpdate(event.getIncidentId(), event.getPayload());
                webSocketEventRelay.pushActiveIncidentsUpdate(event.getPayload());
                log.info("Updated incident status to ROLLBACK: id={}", incident.getId());
            });
        }, () -> log.warn("Action not found for rollback: id={}", actionId));
    }


//helper ====================================================================================================
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

    private boolean isNextInSequence(Long lastSeq, Long incomingSeq) {
        if (lastSeq == null) return true;
        if (incomingSeq == null) return false;
        return incomingSeq > lastSeq;
    }

}
