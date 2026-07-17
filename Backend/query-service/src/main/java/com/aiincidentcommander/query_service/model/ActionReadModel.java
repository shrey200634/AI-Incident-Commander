package com.aiincidentcommander.query_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "action_read")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActionReadModel {
    @Id
    private Long id;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false)
    private String actionType;

    @Column(length = 1000)
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionStatus status;

    private String approvedBy;
    private LocalDateTime executedAt;
    private Long rollbackOf;

    @Column(nullable = false)

    private LocalDateTime createdAt;

    private LocalDateTime lastUpdatedAt;
}
