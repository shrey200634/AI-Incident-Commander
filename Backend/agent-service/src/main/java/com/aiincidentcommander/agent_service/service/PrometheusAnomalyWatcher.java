package com.aiincidentcommander.agent_service.service;

import com.aiincidentcommander.agent_service.client.CommandServiceClient;
import com.aiincidentcommander.agent_service.client.MetricsClient;
import com.aiincidentcommander.agent_service.dto.CreateIncidentDto;
import com.aiincidentcommander.agent_service.dto.MetricSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class PrometheusAnomalyWatcher {

    private final MetricsClient metricsClient;
    private final CommandServiceClient commandServiceClient;
    private final List<String> monitoredServices;

    private final Map<String, Long> lastTriggeredMap = new ConcurrentHashMap<>();
    private static final long INCIDENT_COOLDOWN_MS = 3 * 60 * 1000; // 3 minutes cooldown

    private final double latencyThreshold;
    private final double errorThreshold;

    public PrometheusAnomalyWatcher(
            MetricsClient metricsClient,
            CommandServiceClient commandServiceClient,
            @Value("${monitored.services:api-service,scheduler-service,worker-service}") String monitoredServicesRaw,
            @Value("${anomaly.latency-threshold-ms:50.0}") double latencyThreshold,
            @Value("${anomaly.error-threshold:5.0}") double errorThreshold) {
        this.metricsClient = metricsClient;
        this.commandServiceClient = commandServiceClient;
        this.latencyThreshold = latencyThreshold;
        this.errorThreshold = errorThreshold;
        this.monitoredServices = Arrays.stream(monitoredServicesRaw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Scheduled(fixedRate = 15000) // Poll Prometheus every 15 seconds
    public void monitorPrometheusMetrics() {
        for (String serviceName : monitoredServices) {
            try {
                MetricSnapshot metrics = metricsClient.fetchMetrics(serviceName);

                if (metrics == null || metrics.isStale()) {
                    // Circuit breaker is open or Prometheus is down; skip auto-trigger
                    continue;
                }

                double errorRate = metrics.getErrorRate() != null ? metrics.getErrorRate() : 0.0;
                double latency = metrics.getAvgLatencyMs() != null ? metrics.getAvgLatencyMs() : 0.0;

                boolean errorRateBreach = errorRate > errorThreshold;
                boolean latencyBreach = latency > latencyThreshold;

                if (errorRateBreach || latencyBreach) {
                    long now = System.currentTimeMillis();
                    long lastTriggered = lastTriggeredMap.getOrDefault(serviceName, 0L);

                    if (now - lastTriggered > INCIDENT_COOLDOWN_MS) {
                        String severity = errorRateBreach ? "CRITICAL" : "HIGH";
                        log.warn("🚨 ANOMALY DETECTED by Prometheus Monitor on service [{}]: errorRate={}%, avgLatencyMs={}ms. Auto-creating incident...",
                                serviceName, String.format("%.2f", errorRate), String.format("%.0f", latency));

                        lastTriggeredMap.put(serviceName, now);

                        commandServiceClient.createIncident(CreateIncidentDto.builder()
                                .serviceName(serviceName)
                                .severity(severity)
                                .build());
                    } else {
                        log.debug("Anomaly detected for [{}] but service is under cooldown.", serviceName);
                    }
                }
            } catch (Exception e) {
                log.debug("Prometheus watcher check skipped for {}: {}", serviceName, e.getMessage());
            }
        }
    }
}
