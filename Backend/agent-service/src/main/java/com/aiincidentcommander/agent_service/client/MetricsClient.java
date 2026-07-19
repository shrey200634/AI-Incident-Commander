package com.aiincidentcommander.agent_service.client;


import com.aiincidentcommander.agent_service.dto.MetricSnapshot;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class MetricsClient {

    private final RedisTemplate<String , MetricSnapshot> redisTemplate ;
    private static final String REDIS_KEY_PREFIX = "metrics";

    @CircuitBreaker(name = "metricsService" , fallbackMethod = "fallbackToCache")
    public MetricSnapshot fetchMetrics(String serviceName){
        log.info("Fetching live metrics for {}", serviceName);

        if (new Random().nextInt(10) <4){
            throw new RuntimeException("simulated Prometheus timeout ");
        }
        MetricSnapshot snapshot = MetricSnapshot.builder()
                .serviceName(serviceName)
                .errorRate(new Random().nextDouble() *20)
                .avgLatencyMs(100.0 + new Random().nextInt(400))
                .stale(false)
                .build();

        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + serviceName , snapshot,5, TimeUnit.MINUTES);
        return snapshot;

    }

    private MetricSnapshot fallBackCache(String serviceName , Throwable t ){
        log.warn("Circuit open/call failed for {}, falling back to Redis cache. Reason: {}",
                serviceName, t.getMessage());
        MetricSnapshot cache = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + serviceName);
        if (cache != null ){
            cache.setStale(true);
            return cache;
        }

        return MetricSnapshot.builder()
                .serviceName(serviceName)
                .errorRate(0.0)
                .avgLatencyMs(0.0)
                .stale(true)
                .build();

    }
}
