package com.aiincidentcommander.agent_service.service;

import com.aiincidentcommander.agent_service.client.CommandServiceClient;
import com.aiincidentcommander.agent_service.dto.ProposedActionDto;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Slf4j
@Setter
@RequiredArgsConstructor
public class AgentTools {

    private final CommandServiceClient commandServiceClient;
    private Long currentIncidentId;



    @Tool(description = "Get the overall health/status of a service")
    public String getServiceHealth(String serviceName) {
        log.info("[TOOL] getServiceHealth({})", serviceName);
        String[] statuses = {"HEALTHY", "DEGRADED", "UNHEALTHY"};
        return statuses[new Random().nextInt(statuses.length)];
    }

    @Tool(description = "Get current Kafka consumer lag for a topic")
    public long getKafkaConsumerLag(String topic) {
        log.info("[TOOL] getKafkaConsumerLag({})", topic);
        return new Random().nextInt(5000);
    }

    @Tool(description = "Get Redis memory usage and eviction stats")
    public String getRedisMemoryStats() {
        log.info("[TOOL] getRedisMemoryStats()");
        return String.format("usedMemoryMb=%d, evictedKeys=%d",
                100 + new Random().nextInt(900), new Random().nextInt(50));
    }

    @Tool(description = "Check recent deadlock count from MySQL")
    public int checkDatabaseDeadlocks() {
        log.info("[TOOL] checkDatabaseDeadlocks()");
        return new Random().nextInt(5);
    }

    @Tool(description = "Get recent deployment history for a service, for correlation")
    public String getRecentDeployments(String serviceName) {
        log.info("[TOOL] getRecentDeployments({})", serviceName);
        return new Random().nextBoolean()
                ? "Deployed 12 minutes ago (version 2.4.1)"
                : "No deployments in the last 24 hours";
    }

    @Tool(description = "Propose a remediation action for the current incident. " +
            "This is the ONLY way to act on an incident -- it does not execute anything, " +
            "it only proposes and requires human approval.")
    public String proposeAction(String actionType, String rationale) {
        log.info("[TOOL] proposeAction(actionType={}, rationale={})", actionType, rationale);
        if (currentIncidentId == null) {
            return "Error: no incident context set";
        }
        ProposedActionDto dto = ProposedActionDto.builder()
                .actionType(actionType)
                .rationale(rationale)
                .build();
        commandServiceClient.proposeAction(currentIncidentId, dto);
        return "Action proposed successfully and is now awaiting human approval.";
    }
}