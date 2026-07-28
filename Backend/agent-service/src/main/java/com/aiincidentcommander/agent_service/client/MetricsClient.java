package com.aiincidentcommander.agent_service.client;


import com.aiincidentcommander.agent_service.dto.MetricSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MetricsClient {

    private final RedisTemplate<String, MetricSnapshot> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient prometheusRestClient;

    private static final String REDIS_KEY_PREFIX = "metrics";

    public MetricsClient(RedisTemplate<String, MetricSnapshot> redisTemplate,
                         ObjectMapper objectMapper,
                         @Qualifier("prometheusRestClient") RestClient prometheusRestClient) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.prometheusRestClient = prometheusRestClient;
    }

    @CircuitBreaker(name = "metricsService", fallbackMethod = "fallbackToCache")
    public MetricSnapshot fetchMetrics(String serviceName) {
        log.info("Fetching live metrics from Prometheus for {}", serviceName);

        Double errorRate = queryPrometheusScalar(
                buildErrorRateQuery(serviceName)
        );

        Double avgLatencyMs = queryPrometheusScalar(
                buildAvgLatencyQuery(serviceName)
        );

        // Convert seconds → milliseconds if Prometheus returns latency in seconds
        if (avgLatencyMs != null) {
            avgLatencyMs = avgLatencyMs * 1000;
        }

        MetricSnapshot snapshot = MetricSnapshot.builder()
                .serviceName(serviceName)
                .errorRate(errorRate != null ? errorRate : 0.0)
                .avgLatencyMs(avgLatencyMs != null ? avgLatencyMs : 0.0)
                .stale(false)
                .build();

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + serviceName, snapshot, 5, TimeUnit.MINUTES);
        return snapshot;
    }

    /**
     * Queries the Prometheus /api/v1/query endpoint and extracts the scalar value from the response.
     */
    private Double queryPrometheusScalar(String promQL) {
        try {
            String response = prometheusRestClient.get()
                    .uri("/api/v1/query?query={query}", promQL)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode result = root.path("data").path("result");

            if (result.isArray() && !result.isEmpty()) {
                // Instant vector: result[0].value[1] is the scalar value
                String valueStr = result.get(0).path("value").get(1).asText();
                double val = Double.parseDouble(valueStr);
                return Double.isNaN(val) || Double.isInfinite(val) ? null : val;
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse Prometheus response for query [{}]: {}", promQL, e.getMessage());
            throw new RuntimeException("Prometheus query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Error rate = (5xx requests / total requests) * 100 over the last 5 minutes.
     */
    private String buildErrorRateQuery(String serviceName) {
        return String.format(
                "sum(rate(http_requests_total{job=\"%s\",status=~\"5..\"}[5m])) / " +
                "sum(rate(http_requests_total{job=\"%s\"}[5m])) * 100",
                serviceName, serviceName
        );
    }

    /**
     * Average latency in seconds over the last 5 minutes.
     */
    private String buildAvgLatencyQuery(String serviceName) {
        return String.format(
                "sum(rate(http_request_duration_seconds_sum{job=\"%s\"}[5m])) / " +
                "sum(rate(http_request_duration_seconds_count{job=\"%s\"}[5m]))",
                serviceName, serviceName
        );
    }

    private MetricSnapshot fallbackToCache(String serviceName, Throwable t) {
        log.warn("Circuit open/call failed for {}, falling back to Redis cache. Reason: {}",
                serviceName, t.getMessage());

        Object cached = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + serviceName);
        MetricSnapshot snapshot = null;

        if (cached instanceof MetricSnapshot ms) {
            snapshot = ms;
        } else if (cached != null) {
            snapshot = objectMapper.convertValue(cached, MetricSnapshot.class);
        }

        if (snapshot != null) {
            snapshot.setStale(true);
            return snapshot;
        }

        return MetricSnapshot.builder()
                .serviceName(serviceName)
                .errorRate(0.0)
                .avgLatencyMs(0.0)
                .stale(true)
                .build();
    }
}

