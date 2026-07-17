package com.aiincidentcommander.command_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateStatusRequest {

    @NotBlank(message = "target status is required")
    private String targetStatus;

    private String reason;
}
