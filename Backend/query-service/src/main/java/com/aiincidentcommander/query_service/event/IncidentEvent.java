package com.aiincidentcommander.query_service.event;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentEvent {
    private Long incidentId ;
    private Long sequenceNumber ;
    private LocalDateTime timestamp ;
    private String eventType ;
    private Object payload ;
}
