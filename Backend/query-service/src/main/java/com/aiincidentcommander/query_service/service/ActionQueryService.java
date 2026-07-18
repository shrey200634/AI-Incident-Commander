package com.aiincidentcommander.query_service.service;

import com.aiincidentcommander.query_service.dto.ActionReadDto;
import com.aiincidentcommander.query_service.exception.ActionNotFoundException;
import com.aiincidentcommander.query_service.model.ActionReadModel;
import com.aiincidentcommander.query_service.repo.ActionReadRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ActionQueryService {

    private ActionReadRepo actionReadRepo ;

    // get all action for an incident
    public List<ActionReadDto> getActionByIncidentId(Long id){
        log.info("Fetching actions for incident: id={}", id);
        return actionReadRepo.findByIncidentId(id)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // get single action by id
    public ActionReadDto getActionById (Long id ){
        log.info("Fetching action: id={}", id);
        ActionReadModel action = actionReadRepo.findById(id)
                .orElseThrow(()-> new ActionNotFoundException(id));
        return toDTO(action);
    }


    //helper ------------------------------------------------------------------------------------------------

    private ActionReadDto toDTO (ActionReadModel model){
        return ActionReadDto.builder()
                .id(model.getId())
                .incidentId(model.getIncidentId())
                .actionType(model.getActionType())
                .rationals(model.getRationale())
                .status(model.getStatus())
                .approvedBy(model.getApprovedBy())
                .executedAt(model.getExecutedAt())
                .rollBackOf(model.getRollbackOf())
                .createdAt(model.getCreatedAt())
                .lastUpdatedAt(model.getLastUpdatedAt())
                .build();
    }
}
