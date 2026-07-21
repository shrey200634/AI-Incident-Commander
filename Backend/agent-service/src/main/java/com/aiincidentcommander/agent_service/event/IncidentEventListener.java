package com.aiincidentcommander.agent_service.event;

import com.aiincidentcommander.agent_service.service.AgentTools;
import com.aiincidentcommander.agent_service.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class IncidentEventListener {

         private final AiService aiService ;
         private final AgentTools tools ;

         @KafkaListener(topics = "incident.created" , groupId = "agent-service-group")
          public  void onIncidentCreated(IncidentEvent event){
             log.info("Agent received incident.created: incidentId={}", event.getIncidentId());
             tools.setCurrentIncidentId(event.getIncidentId());

             String serviceName = extractField(event , "serviceName" , "unknown-service");
             String severity = extractField(event , "severity" , "UNKNOWN");

             String prompt = """
                      An incident was just created:
                                     - Service: %s
                                     - Severity: %s
                                     - Incident ID: %d
                     
                                     Investigate using the available read-only diagnostic tools before
                                     deciding on anything. Call as many diagnostic tools as you need to
                                     form a grounded hypothesis. Once you're confident, call proposeAction
                                     with the most appropriate remediation type and a clear rationale
                                     citing what you found. If nothing looks wrong, do not propose an
                                     action -- just explain why.
                                     """.formatted(serviceName,severity,event.getIncidentId());

             String res = aiService.call(prompt,tools);
             log.info("Agent reasoning result for incidentId={}: {}", event.getIncidentId(), res);


         }

         @SuppressWarnings("unchecked")
    private String extractField(IncidentEvent event , String field , String fallBack ){
             if (event.getPayload() instanceof Map<?,?> payloadMap){
                 Object value = payloadMap.get(field);
                 if (value != null) return value.toString();
             }
             return fallBack;
         }
}
