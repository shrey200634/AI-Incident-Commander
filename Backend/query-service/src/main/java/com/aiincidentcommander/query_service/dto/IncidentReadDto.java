package com.aiincidentcommander.query_service.dto;

import com.aiincidentcommander.query_service.model.IncidentStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
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
