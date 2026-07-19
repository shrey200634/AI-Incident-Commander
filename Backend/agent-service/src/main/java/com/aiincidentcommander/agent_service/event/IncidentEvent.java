package com.aiincidentcommander.agent_service.event;

import lombok.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncidentEvent {
    private Long incidentId ;
    private Long sequenceNumber ;
    private LocalDateTime timestamp ;
    private String eventType ;
    private Object payload ;


}
