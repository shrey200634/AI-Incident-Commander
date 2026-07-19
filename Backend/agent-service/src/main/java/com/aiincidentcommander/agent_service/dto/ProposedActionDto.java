package com.aiincidentcommander.agent_service.dto;

import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProposedActionDto {
    private String actionType ;
    private String rationals;
}
