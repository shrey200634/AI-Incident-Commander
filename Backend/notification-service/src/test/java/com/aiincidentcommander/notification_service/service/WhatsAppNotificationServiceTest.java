package com.aiincidentcommander.notification_service.service;

import com.aiincidentcommander.notification_service.model.NotificationLog;
import com.aiincidentcommander.notification_service.model.NotificationStatus;
import com.aiincidentcommander.notification_service.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhatsAppNotificationServiceTest {

    @Mock
    private TwilioConfig twilioConfig;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private WhatsAppNotificationService whatsappNotificationService;

    @BeforeEach
    void setUp() {
        lenient().when(twilioConfig.getFromNumber()).thenReturn("whatsapp:+14155238886");
        lenient().when(twilioConfig.getToNumber()).thenReturn("whatsapp:+919876543210");
    }

    @Test
    @DisplayName("Should save log entry with FAILD status when Twilio authentication fails in test environment")
    void testSend_TwilioFailureHandled() {
        Long incidentId = 101L;
        String eventType = "action.proposed";
        String messageBody = "Test message payload";

        whatsappNotificationService.send(incidentId, eventType, messageBody);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(1)).save(logCaptor.capture());

        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(incidentId, savedLog.getIncidentId());
        assertEquals("WHATSAPP", savedLog.getChanel());
        assertEquals(eventType, savedLog.getEventType());
        assertEquals(messageBody, savedLog.getMessageBody());
        assertEquals(NotificationStatus.FAILD, savedLog.getStatus());
        assertNotNull(savedLog.getFailureReason());
    }
}
