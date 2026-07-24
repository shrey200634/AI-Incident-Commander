package com.aiincidentcommander.command_service.service;


import com.aiincidentcommander.command_service.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "idempotency:";
    private static final long TTL_HOURS = 24;

    public Optional<String> get(String idempotency){
        String value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotency);
        return Optional.ofNullable(value);
    }

    public  void store(String idempotencyKey , String responseJson){
        redisTemplate.opsForValue().set(KEY_PREFIX+idempotencyKey , responseJson,TTL_HOURS, TimeUnit.HOURS);
    }
}
