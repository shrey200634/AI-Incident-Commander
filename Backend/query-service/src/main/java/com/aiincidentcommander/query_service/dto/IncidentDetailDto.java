package com.aiincidentcommander.query_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class IncidentDetailDto {

    private IncidentReadDto incident ;
    private List<ActionReadDto> actions ;
}
