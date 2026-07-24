package com.aiincidentcommander.command_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectActionRequest {
    private String reason;
}