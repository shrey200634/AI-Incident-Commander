package com.aiincidentcommander.command_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "remediation_actions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemediationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    // nullable — set only when this row is a compensating rollback of another action
    private Long rollbackOf;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}