package com.aiincidentcommander.query_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "dlq_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlqRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_topic", nullable = false)
    private String originalTopic;

    @Column(name = "kafka_partition", nullable = false)
    private int partition;

    @Column(name = "kafka_offset", nullable = false)
    private long offset;

    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "replayed", nullable = false)
    private boolean replayed;

    @Column(name = "failed_at", nullable = false, updatable = false)
    private LocalDateTime failedAt;

    @Column(name = "replayed_at")
    private LocalDateTime replayedAt;

    @PrePersist
    protected void onCreate() {
        this.failedAt = LocalDateTime.now();
        this.replayed = false;
    }
}