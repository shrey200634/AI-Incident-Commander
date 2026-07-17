package com.aiincidentcommander.command_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionProposed {


    @NotBlank(message = " action type is requires ")
    private String actionType ;

    @NotBlank(message = " rational is required ")
    private String rational ;
}
