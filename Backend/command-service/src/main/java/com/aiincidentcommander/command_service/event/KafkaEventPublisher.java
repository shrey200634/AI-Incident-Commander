package com.aiincidentcommander.command_service.event;


import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPublisher {
    private final KafkaTemplate<String , Object> kafkaTemplate ;

    public  void publish(String topic , Long incidentId , IncidentEvent event ){

        kafkaTemplate.send(topic , String.valueOf(event.getIncidentId()),event);

    }
}
