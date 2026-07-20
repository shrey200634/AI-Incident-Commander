package com.aiincidentcommander.agent_service.controller;


import com.aiincidentcommander.agent_service.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AiTestController {

    private final AiService service ;

    @GetMapping("/test/ai")
    public  String testAi(@RequestParam(defaultValue = "say hellow in one sentence ") String prompt ){
        return service.call(prompt);
    }

}
