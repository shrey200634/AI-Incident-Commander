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

    private final RateLimitProperties properties ;
    private final ConcurrentHashMap<String , Bucket> buckets = new ConcurrentHashMap<>();

    public  Bucket resolveBucket(String clientKey){
        return buckets.computeIfAbsent(clientKey, key->newBucket());
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(
                properties.getCapacity(),
                Refill.greedy(properties.getRefillToken(), Duration.ofSeconds(properties.getRefillSecond()))
        );
        return Bucket.builder().addLimit(limit).build();
    }
}
