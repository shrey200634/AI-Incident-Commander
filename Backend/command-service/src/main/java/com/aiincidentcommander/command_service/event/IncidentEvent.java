package com.aiincidentcommander.command_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class IncidentEvent {

    private Long incidentId ;
    private Long sequenceNumber ;
    private LocalDateTime timestamp ;
    private String eventType ;
    private Object payload ;

}
