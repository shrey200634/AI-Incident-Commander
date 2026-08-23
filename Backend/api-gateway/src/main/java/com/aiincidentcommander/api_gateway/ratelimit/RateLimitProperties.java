package com.aiincidentcommander.api_gateway.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private int capacity = 20;
    private int refillToken =20 ;
    private int refillSecond =60 ;

}
