package com.aiincidentcommander.agent_service.controller;

import com.aiincidentcommander.agent_service.client.MetricsClient;
import com.aiincidentcommander.agent_service.dto.MetricSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MetricsTestController {

    private final  MetricsClient metricsClient ;

    @GetMapping("/test/metrics")
    public MetricSnapshot getMetrics(@RequestParam(defaultValue = "FoodRush-Orders") String serviceName ){
        return metricsClient.fetchMetrics(serviceName);
    }
}
