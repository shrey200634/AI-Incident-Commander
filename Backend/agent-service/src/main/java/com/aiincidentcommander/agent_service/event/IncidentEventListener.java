package com.aiincidentcommander.agent_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentEventListener {

    @KafkaListener(topics = "incident.created" , groupId = "agent-service-group")
    public  void onIncidentCreated(IncidentEvent event){
        log.info("Agent received incident.created: incidentId={}", event.getIncidentId());


    }
}
