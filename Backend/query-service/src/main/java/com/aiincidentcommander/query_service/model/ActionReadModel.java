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

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "rationale", length = 1000)
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ActionStatus status;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "rollback_of")
    private Long rollbackOf;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_updated_at")
    private LocalDateTime lastUpdatedAt;

    @Column(name = "last_sequence_number")
    private Long lastSequenceNumber;
}
