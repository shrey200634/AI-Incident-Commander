package com.aiincidentcommander.command_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionProposed {

    @NotBlank(message = "action type is required")
    private String actionType;

    @NotBlank(message = "rationale is required")
    private String rationale;
}
