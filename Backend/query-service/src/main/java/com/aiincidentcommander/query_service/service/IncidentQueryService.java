package com.aiincidentcommander.query_service.service;

import com.aiincidentcommander.query_service.dto.ActionReadDto;
import com.aiincidentcommander.query_service.dto.IncidentDetailDto;
import com.aiincidentcommander.query_service.dto.IncidentReadDto;
import com.aiincidentcommander.query_service.exception.IncidentNotFoundException;
import com.aiincidentcommander.query_service.model.ActionReadModel;
import com.aiincidentcommander.query_service.model.IncidentReadModel;
import com.aiincidentcommander.query_service.model.IncidentStatus;
import com.aiincidentcommander.query_service.repo.ActionReadRepo;
import com.aiincidentcommander.query_service.repo.IncidentReadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentQueryService {

    private final IncidentReadRepository incidentReadRepository ;
    private final ActionReadRepo actionReadRepo;

    //get all the incident
    public List<IncidentReadDto> getAllIncident (){
        log.info("Fetching all the incident ");
        return incidentReadRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // get Incident by id
    public  IncidentReadDto getIncidentById (Long id ){
        log.info("fetching incident : id={}" , id);
        IncidentReadModel incident = findOrThrow(id);
        return toDTO(incident);
    }

    //get Incident details
    public IncidentDetailDto getIncidentDetail(Long id) {
        log.info("Fetching incident detail: id={}", id);
        IncidentReadModel incident = findOrThrow(id);
        List<ActionReadDto> actions = actionReadRepo.findByIncidentId(id)
                .stream()
                .map(this::toActionDTO)
                .toList();
        return IncidentDetailDto.builder()
                .incident(toDTO(incident))
                .actions(actions)
                .build();
    }

    //filter by status
    public  List<IncidentReadDto> getIncidentByStatus (IncidentStatus status){
        log.info("Fetching incidents by status: {}", status);
        return incidentReadRepository.findByStatus(status)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    //filter by severity
    public  List<IncidentReadDto> getBySeverity(String severity){
        log.info("Fetching incidents by severity: {}", severity);
        return incidentReadRepository.findBySeverity(severity)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    //find by serviceName
    public List<IncidentReadDto> getByServiceName (String serviceName ){
        log.info("Fetching incidents by serviceName: {}", serviceName);
        return incidentReadRepository.findByServiceName(serviceName)
                .stream()
                .map(this::toDTO)
                .toList();
    }


    //get active (non resolved / non esclated )
    public List<IncidentReadDto> getActiveIncident(){
        log.info("fetching active incident ");
        List<IncidentStatus> activeStatus = List.of(
                IncidentStatus.NEW,
                IncidentStatus.INVESTIGATING,
                IncidentStatus.ACTION_PROPOSED,
                IncidentStatus.WAITING_APPROVAL,
                IncidentStatus.EXECUTING,
                IncidentStatus.MONITORING,
                IncidentStatus.ROLLBACK
        );
        return incidentReadRepository.findByStatusIn(activeStatus)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    //helper--------------------------------------------------------------------------------------------------
    private IncidentReadModel findOrThrow(Long id ){
        return incidentReadRepository.findById(id)
                .orElseThrow(()-> new IncidentNotFoundException(id));
    }

    private IncidentReadDto toDTO(IncidentReadModel model){
        return IncidentReadDto.builder()
                .id(model.getId())
                .serviceName(model.getServiceName())
                .severity(model.getSeverity())
                .status(model.getStatus())
                .createdAt(model.getCreatedAt())
                .resolvedAt(model.getResolvedAt())
                .escalationReason(model.getEscalationReason())
                .lastUpdated(model.getLastUpdatedAt())
                .build();
    }

    private ActionReadDto toActionDTO(ActionReadModel model){
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
