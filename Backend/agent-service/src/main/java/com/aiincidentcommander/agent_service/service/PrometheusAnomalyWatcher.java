package com.aiincidentcommander.agent_service.service;

import com.aiincidentcommander.agent_service.client.CommandServiceClient;
import com.aiincidentcommander.agent_service.client.MetricsClient;
import com.aiincidentcommander.agent_service.dto.CreateIncidentDto;
import com.aiincidentcommander.agent_service.dto.MetricSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class PrometheusAnomalyWatcher {

    private final MetricsClient metricsClient;
    private final CommandServiceClient commandServiceClient;

    private static final List<String> MONITORED_SERVICES = List.of("FoodRush-Orders", "Payment-Gateway");
    private final Map<String, Long> lastTriggeredMap = new ConcurrentHashMap<>();
    private static final long INCIDENT_COOLDOWN_MS = 3 * 60 * 1000; // 3 minutes cooldown

    @Scheduled(fixedRate = 15000) // Poll Prometheus every 15 seconds
    public void monitorPrometheusMetrics() {
        for (String serviceName : MONITORED_SERVICES) {
            try {
                MetricSnapshot metrics = metricsClient.fetchMetrics(serviceName);

                if (metrics == null || metrics.isStale()) {
                    // Circuit breaker is open or Prometheus is down; skip auto-trigger
                    continue;
                }

                double errorRate = metrics.getErrorRate() != null ? metrics.getErrorRate() : 0.0;
                double latency = metrics.getAvgLatencyMs() != null ? metrics.getAvgLatencyMs() : 0.0;

                boolean errorRateBreach = errorRate > 5.0; // > 5% error rate
                boolean latencyBreach = latency > 500.0;  // > 500ms latency

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
