package com.aiincidentcommander.agent_service.event;

import com.aiincidentcommander.agent_service.client.CommandServiceClient;
import com.aiincidentcommander.agent_service.dto.ProposedActionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentEventListener {

    private final  CommandServiceClient client;

    @KafkaListener(topics = "incident.created" , groupId = "agent-service-group")
    public  void onIncidentCreated(IncidentEvent event){
        log.info("Agent received incident.created: incidentId={}", event.getIncidentId());
        ProposedActionDto request = ProposedActionDto.builder()
                .actionType("RESTART_SERVICE")
                .rationale("Auto-proposed by agent based on incident creation (placeholder logic)")
                .build();

          client.proposeAction(event.getIncidentId(), request);
    }
}
