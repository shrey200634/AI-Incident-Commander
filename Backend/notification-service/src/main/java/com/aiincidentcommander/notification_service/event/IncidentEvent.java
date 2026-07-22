package com.aiincidentcommander.notification_service.event;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class IncidentEvent {

    private Long incidentId ;
    private Long sequenceNumber ;
    private LocalDateTime timestamp ;
    private String eventType ;
    private Object payload ;

}
