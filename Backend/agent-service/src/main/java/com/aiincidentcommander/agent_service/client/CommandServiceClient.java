package com.aiincidentcommander.agent_service.client;


import com.aiincidentcommander.agent_service.dto.ProposedActionDto;
import com.aiincidentcommander.agent_service.dto.RemediationActionResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class CommandServiceClient {

    private final RestClient restClient ;

    public CommandServiceClient(@Value("${command-service.base-url}") String baseUrl) {
        this.restClient =RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    public RemediationActionResponse proposeAction(Long incidentId, ProposedActionDto request) {
        log.info("Proposing action for incidentId={}: type={}", incidentId, request.getActionType());

        return  restClient.post()
                .uri("/api/incidents/{id}/actions", incidentId)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(RemediationActionResponse.class);
    }

    }
