package com.aiincidentcommander.api_gateway.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;


    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();

    public Bucket resolveBucket(RateLimitTier tier, String clientKey) {
        return switch (tier) {
            case AUTH -> authBuckets.computeIfAbsent(clientKey, k -> newBucket(properties.getAuth()));
            case GENERAL -> generalBuckets.computeIfAbsent(clientKey, k -> newBucket(properties.getGeneral()));
        };
    }

    private Bucket newBucket(RateLimitProperties.Tier tierConfig) {
        Bandwidth limit = Bandwidth.classic(
                tierConfig.getCapacity(),
                Refill.greedy(tierConfig.getRefillTokens(), Duration.ofSeconds(tierConfig.getRefillSeconds()))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    public int capacityFor(RateLimitTier tier) {
        return switch (tier) {
            case AUTH -> properties.getAuth().getCapacity();
            case GENERAL -> properties.getGeneral().getCapacity();
        };
    }
}