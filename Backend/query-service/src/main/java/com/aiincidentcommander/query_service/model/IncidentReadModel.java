package com.aiincidentcommander.query_service.model;

import jakarta.persistence.*;
import lombok.*;

import javax.sql.rowset.spi.SyncResolver;
import java.time.LocalDateTime;

@Entity
@Table(name ="incident_read")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class IncidentReadModel {
    @Id
    private Long id ;

    @Column(nullable = false)
    private String serviceName ;

    @Column(nullable = false)
    private String severity ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status ;

    @Column(nullable = false)
    private LocalDateTime createdAt ;

    private LocalDateTime resolvedAt ;
    private String escalationReason ;
    private LocalDateTime lastUpdatedAt ;


}
