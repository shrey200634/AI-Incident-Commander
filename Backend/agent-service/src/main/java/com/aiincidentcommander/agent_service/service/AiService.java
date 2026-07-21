package com.aiincidentcommander.agent_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiService {

    private final ChatClient geminiChatClint ;
    private  final ChatClient groqClient ;
    public AiService(@Qualifier("geminiChatClint") ChatClient geminiChatClint ,@Qualifier("groqChatClient") ChatClient groqClient){
        this.geminiChatClint=geminiChatClint;
        this.groqClient=groqClient;

    }

    public  String call(String prompt  , Object... tools ){
        try {
            log.info("Calling Gemini (primary)");
            return geminiChatClint
                    .prompt()
                    .user(prompt)
                    .tools(tools)
                    .call()
                    .content();
        }catch (Exception ex ){
            log.warn("Gemini call failed ({}), falling back to Groq", ex.getMessage());
            try{
            return groqClient.prompt()
                    .user(prompt)
                    .tools(tools)
                    .call()
                    .content();
            }catch (Exception e2){
                log.error("Groq fallback also failed: {}", e2.getMessage());
                 throw new RuntimeException("Both gemini and groq is failed " , e2);
            }
        }
    }
}
