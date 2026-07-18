package com.aiincidentcommander.query_service.dto;

import com.aiincidentcommander.query_service.model.ActionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActionReadDto {
    private Long id ;
    private Long incidentId ;
    private String actionType ;
    private String rationals ;
    private ActionStatus status ;
    private String approvedBy ;
    private LocalDateTime executedAt ;
    private Long rollBackOf ;
    private LocalDateTime createdAt ;
    private LocalDateTime lastUpdatedAt ;
}
