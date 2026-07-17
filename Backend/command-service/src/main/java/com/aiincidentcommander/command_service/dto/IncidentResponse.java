package com.aiincidentcommander.command_service.dto;

import com.aiincidentcommander.command_service.model.IncidentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class IncidentResponse {

    private Long id ;
    private String serviceName ;
    private String severity;
    private IncidentStatus status ;
    private LocalDateTime createdAt ;

}
