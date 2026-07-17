package com.aiincidentcommander.command_service.dto;

import com.aiincidentcommander.command_service.model.ActionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RemediationActionResponse {

    private Long id;
    private Long incidentId;
    private String actionType;
    private String rationale;
    private ActionStatus status;
    private LocalDateTime createdAt;
}
