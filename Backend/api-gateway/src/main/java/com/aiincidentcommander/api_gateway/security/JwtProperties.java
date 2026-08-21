package com.aiincidentcommander.api_gateway.security;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private  String secret;
    private long expirationMs;
}
