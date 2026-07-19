package com.aiincidentcommander.agent_service.dto;

import lombok.*;


@Builder
@Setter
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class MetricSnapshot {


    private String serviceName ;
    private Double errorRate ;
    private Double avgLatencyMs ;
    private boolean stale ; // true when served from redis fallback , not live Prometheus

}
