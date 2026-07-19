package com.aiincidentcommander.agent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor

public class RemediationActionResponse {
    private Long id ;
    private Long incidentId ;
    private String actionType ;
    private String status ;
}
