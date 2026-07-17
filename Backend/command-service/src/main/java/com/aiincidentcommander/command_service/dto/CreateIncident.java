package com.aiincidentcommander.command_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateIncident {

    @NotBlank(message = "service name is required ")
    private String serviceName ;


    @NotBlank(message = " severity is required")
    private String severity ;
}
