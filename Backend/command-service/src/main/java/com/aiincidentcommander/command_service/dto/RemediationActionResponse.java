package com.aiincidentcommander.command_service.dto;


import com.aiincidentcommander.command_service.model.ActionStatus;
import com.aiincidentcommander.command_service.model.IncidentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RemediationActionResponse {

    private Long id ;
    private Long incidentId ;
    private String actionType ;
    private String rational ;
    private ActionStatus status ;
    private LocalDateTime createdAt ;
}
