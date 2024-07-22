package com.zayn.dianping.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * redisson 配置类
 *
 * @author zayn
 * * @date 2024/7/22/下午1:28
 */
@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String REDIS_HOST;
    @Value("${spring.data.redis.port}")
    private String REDIS_PORT;
    @Value("${spring.data.redis.password}")
    private String REDIS_PASSWORD;
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + REDIS_HOST + ":" + REDIS_PORT)
              .setPassword(REDIS_PASSWORD);
        return Redisson.create();
    }
}
