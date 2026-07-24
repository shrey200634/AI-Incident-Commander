package com.aiincidentcommander.notification_service.service;

import com.aiincidentcommander.notification_service.model.NotificationLog;
import com.aiincidentcommander.notification_service.model.NotificationStatus;
import com.aiincidentcommander.notification_service.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${alert.email}")
    private String receiverEmail;

    @Async
    public void send(Long incidentId, String eventType, String messageBody) {

        NotificationLog logEntry = NotificationLog.builder()
                .incidentId(incidentId)
                .chanel("EMAIL")
                .eventType(eventType)
                .messageBody(messageBody)
                .status(NotificationStatus.PENDING)
                .build();

        try {

            SimpleMailMessage message = new SimpleMailMessage();

            message.setFrom(senderEmail);
            message.setTo(receiverEmail);
            message.setSubject(buildSubject(incidentId, eventType));
            message.setText(messageBody);

            mailSender.send(message);

            logEntry.setStatus(NotificationStatus.SENT);

            log.info("Email sent successfully for incidentId={}", incidentId);

        } catch (Exception ex) {

            logEntry.setStatus(NotificationStatus.FAILD); // Keep this if your enum is FAILD

            logEntry.setFailureReason(ex.getMessage());

            log.error("Email failed for incidentId={}: {}", incidentId, ex.getMessage());

        } finally {

            notificationLogRepository.save(logEntry);

        }
    }

    private String buildSubject(Long incidentId, String eventType) {

        return switch (eventType.toUpperCase()) {

            case "INCIDENT_CREATED" ->
                    "🚨 Incident Created | #" + incidentId;

            case "ACTION_PROPOSED" ->
                    "⚠ AI Action Proposed | Incident #" + incidentId;

            case "ACTION_APPROVED" ->
                    "✅ Action Approved | Incident #" + incidentId;

            case "ACTION_EXECUTED" ->
                    "⚙ Action Executed | Incident #" + incidentId;

            case "ACTION_ROLLED_BACK" ->
                    "↩ Action Rolled Back | Incident #" + incidentId;

            case "INCIDENT_ESCALATED" ->
                    "🔥 Incident Escalated | Incident #" + incidentId;

            case "INCIDENT_RESOLVED" ->
                    "✔ Incident Resolved | Incident #" + incidentId;

            default ->
                    "AI Incident Commander Notification";
        };
    }
}