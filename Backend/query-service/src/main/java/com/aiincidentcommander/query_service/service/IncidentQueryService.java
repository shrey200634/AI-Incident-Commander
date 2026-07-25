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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentQueryService {

    private final IncidentReadRepository incidentReadRepository ;
    private final ActionReadRepo actionReadRepo;
    private final RedisTemplate<String , Object> redisTemplate;

    private static final String ACTIVE_INCIDENT_KEY = "incident:active";
    private static final Duration ACTIVE_TTL = Duration.ofSeconds(5);
    private static final String INCIDENT_KEY_PREFIX = "incident:";
    private static  final Duration INCIDENT_TTL = Duration.ofSeconds(10);




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
       String cacheKey = INCIDENT_KEY_PREFIX + id;
       Object cached = redisTemplate.opsForValue().get(cacheKey);
       if (cached!= null){
           log.info("Cache hit: incident id={}", id);
           return (IncidentReadDto) cached;
       }
        log.info("Cache miss, fetching incident from DB: id={}", id);
       IncidentReadModel incidentReadModel = findOrThrow(id);
       IncidentReadDto dto = toDTO(incidentReadModel);

       redisTemplate.opsForValue().set(cacheKey , dto , INCIDENT_TTL);
       return dto;

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
    @SuppressWarnings("unchecked")
    public List<IncidentReadDto> getActiveIncident() {
        Object cached = redisTemplate.opsForValue().get(ACTIVE_INCIDENT_KEY);
        if (cached != null) {
            log.info("Cache hit: active incidents");
            return (List<IncidentReadDto>) cached;
        }

        log.info("Cache miss, fetching active incidents from DB");
        List<IncidentStatus> activeStatus = List.of(
                IncidentStatus.NEW,
                IncidentStatus.INVESTIGATING,
                IncidentStatus.ACTION_PROPOSED,
                IncidentStatus.WAITING_APPROVAL,
                IncidentStatus.EXECUTING,
                IncidentStatus.MONITORING,
                IncidentStatus.ROLLBACK
        );
        List<IncidentReadDto> result = incidentReadRepository.findByStatusIn(activeStatus)
                .stream()
                .map(this::toDTO)
                .toList();

        redisTemplate.opsForValue().set(ACTIVE_INCIDENT_KEY, result, ACTIVE_TTL);
        return result;
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



// called by the Kafka consumers on any write, so the cache never serves stale
    public void evictIncidentCache(Long id) {
        redisTemplate.delete(INCIDENT_KEY_PREFIX + id);
        redisTemplate.delete(ACTIVE_INCIDENT_KEY);
    }
}
