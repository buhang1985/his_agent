package com.hisagent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!redis")
public class NoRedissonConfig {

    @Bean
    public org.redisson.api.RedissonClient redissonClient() {
        return null;
    }
}
