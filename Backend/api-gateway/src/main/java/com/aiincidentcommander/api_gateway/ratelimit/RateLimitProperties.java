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

    private Tier auth = new Tier(10, 10, 60);       // for auth endpoint
    private Tier general = new Tier(300, 300, 60);  //for the general

    @Setter
    @Getter
    public static class Tier {
        private int capacity;
        private int refillTokens;
        private int refillSeconds;

        public Tier() {
        }

        public Tier(int capacity, int refillTokens, int refillSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillSeconds = refillSeconds;
        }
    }
}