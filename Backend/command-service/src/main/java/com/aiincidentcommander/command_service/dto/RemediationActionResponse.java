package com.aiincidentcommander.command_service.dto;

import com.aiincidentcommander.command_service.model.ActionStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RemediationActionResponse {

    private Long id;
    private Long incidentId;
    private String serviceName ;
    private String actionType;
    private String rationale;
    private ActionStatus status;
    private LocalDateTime createdAt;
}
