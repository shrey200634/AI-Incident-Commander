package com.aiincidentcommander.notification_service.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "twilio")
@Setter
@Getter
public class TwilioConfig {

    private String accountId ;
    private String authToken ;
    private String fromNumber ;
    private String toNumber ;

    @PostConstruct
    public  void init(){
        Twilio.init(accountId , authToken);
    }
}
