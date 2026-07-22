package com.aiincidentcommander.notification_service.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    private Long incidentId ;

    private String chanel ;

    private String eventType ;

    private String messageBody ;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status ;

    private String failureReason ;

    @Column(unique = false)
    private LocalDateTime sentAt ;

    @PrePersist
    protected  void onCreate(){
        this.sentAt= LocalDateTime.now();
    }

}
