package com.aiincidentcommander.query_service.event;


import com.aiincidentcommander.query_service.model.ActionReadModel;
import com.aiincidentcommander.query_service.model.ActionStatus;
import com.aiincidentcommander.query_service.repo.ActionReadRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ActionEventConsumer {

    private ObjectMapper objectMapper;
    private ActionReadRepo actionReadRepo;


    // action events
    @KafkaListener(topics = "action.proposed" , groupId = "query-service-group")
    public  void onActionProposed(IncidentEvent event){
        log.info("Received action proposed: incident id ={}" , event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());

        ActionReadModel model = ActionReadModel.builder()
                .id(toLong(payload.get("id")))
                .incidentId(toLong(payload.get("incidentId")))
                .actionType((String) payload.get("actionType"))
                .rationale((String) payload.get("rationals"))
                .status(ActionStatus.valueOf((String) payload.get("status")))
                .createdAt(toLocalDateTime(payload.get("createdAt")))
                .lastUpdatedAt(LocalDateTime.now())
                .build();

        actionReadRepo.save(model);
        log.info("Saved action read model: id={}", model.getId());
    }

    @KafkaListener(topics = "action.approved", groupId = "query-service-group")
    public void onActionApproved(IncidentEvent event) {
        log.info("Received action.approved: incidentId={}", event.getIncidentId());
        Map<String, Object> payload = toMap(event.getPayload());

        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {

            model.setStatus(ActionStatus.APPROVED);
            model.setApprovedBy((String) payload.get("approvedBy"));
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);

            log.info("Updated action to APPROVED: id={}", actionId);
        }, () -> log.warn("Action not found for approval: id={}", actionId));
    }



    @KafkaListener(topics = "action.executed", groupId = "query-service-group")
    public void onActionExecuted(IncidentEvent event) {
        log.info("Received action.executed: incidentId={}", event.getIncidentId());

        Map<String, Object> payload = toMap(event.getPayload());
        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {
            model.setStatus(ActionStatus.EXECUTED);
            model.setExecutedAt(toLocalDateTime(payload.get("executedAt")));
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);
            log.info("Updated action to EXECUTED: id={}", actionId);
        }, () -> log.warn("Action not found for execution: id={}", actionId));
    }


    @KafkaListener(topics = "action.rolled_back", groupId = "query-service-group")
    public void onActionRolledBack(IncidentEvent event) {
        log.info("Received action.rolled_back: incidentId={}", event.getIncidentId());

        Map<String, Object> payload = toMap(event.getPayload());
        Long actionId = toLong(payload.get("id"));
        actionReadRepo.findById(actionId).ifPresentOrElse(model -> {

            model.setStatus(ActionStatus.ROLLED_BACK);
            model.setLastUpdatedAt(LocalDateTime.now());
            actionReadRepo.save(model);
            log.info("Updated action to ROLLED_BACK: id={}", actionId);
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

}
