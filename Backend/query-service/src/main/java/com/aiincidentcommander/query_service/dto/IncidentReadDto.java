package com.aiincidentcommander.query_service.dto;

import com.aiincidentcommander.query_service.model.IncidentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IncidentReadDto {
    private Long id ;
    private String serviceName  ;
    private String severity;
    private IncidentStatus status ;
    private LocalDateTime createdAt ;
    private LocalDateTime resolvedAt ;
    private String escalationReason ;
    private LocalDateTime lastUpdated ;
}
