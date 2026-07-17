package com.aiincidentcommander.command_service.model;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Column(nullable = false)
    private String serviceName ;

    @Column(nullable = false)
    private String severity ;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status ;

    @Column(nullable = false , unique = false)
    private LocalDateTime createdAt ;

    private LocalDateTime resolvedAt ;
    private String escalationReason ;

    @PrePersist
    protected  void oneCreate(){
        this.createdAt = LocalDateTime.now();
        this.status = IncidentStatus.NEW;
    }
}
