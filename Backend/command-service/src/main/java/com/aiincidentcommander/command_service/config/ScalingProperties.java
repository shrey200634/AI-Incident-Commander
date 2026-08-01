package com.aiincidentcommander.command_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "scaling")
@Getter
@Setter
public class ScalingProperties {
    private Map<String, String> pathMappings = new HashMap<>();
}