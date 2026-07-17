package com.aiincidentcommander.command_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveActionRequest {

    @NotBlank(message = "approvedBy is required")
    private String approvedBy;
}
