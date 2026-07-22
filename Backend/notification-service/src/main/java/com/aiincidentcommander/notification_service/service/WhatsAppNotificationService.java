package com.aiincidentcommander.notification_service.service;

import com.aiincidentcommander.notification_service.config.TwilioConfig;
import com.aiincidentcommander.notification_service.model.NotificationLog;
import com.aiincidentcommander.notification_service.model.NotificationStatus;
import com.aiincidentcommander.notification_service.repository.NotificationLogRepository;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.twilio.rest.api.v2010.account.Message;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class WhatsAppNotificationService {

    private final  TwilioConfig twilioConfig ;
    private final  NotificationLogRepository notificationLogRepository;

    @Async
    private void send(Long incidentId , String eventType , String massageBody){
        NotificationLog logEntry = NotificationLog.builder()
                .incidentId(incidentId)
                .chanel("WHATSAPP")
                .eventType(eventType)
                .messageBody(massageBody)
                .status(NotificationStatus.PENDING)
                .build();

        try {
            Message message = Message.creator(
                    new PhoneNumber(twilioConfig.getToNumber()),
                    new PhoneNumber(twilioConfig.getFromNumber()),
                    massageBody
            ).create();
            logEntry.setStatus(NotificationStatus.SENT);
            log.info("WhatsApp sent for incidentId={}, sid={}", incidentId, message.getSid());
        }
        catch (Exception ex ){
            logEntry.setStatus(NotificationStatus.FAILD);
            logEntry.setFailureReason(ex.getMessage());

            log.error("WhatsApp failed for incidentId={}: {}", incidentId, ex.getMessage());

        } finally {
            notificationLogRepository.save(logEntry);
        }
    }
}
