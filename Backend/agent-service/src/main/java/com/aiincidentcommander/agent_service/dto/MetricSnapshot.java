package com.aiincidentcommander.agent_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {


    private String serviceName ;
    private Double errorRate ;
    private Double avgLatencyMs ;
    private boolean stale ; // true when served from redis fallback , not live Prometheus

}
