package com.zayn.dianping.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * redis UUID生成器
 *
 * @author zayn
 * * @date 2024/7/20/下午4:46
 */
@Component
@RequiredArgsConstructor
public class RedisIDGenerator {
    
    private static final long BEGIN_TIMESTAMP = 1704067200L; // 2024-1-1 00:00:00
    private static final int SEQUENCE_BITS = 32;
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 符号位1
     * 时间戳31
     * 序列号32
     *
     * @param keyPrefix key前缀
     * @return id
     */
    public long nextId(String keyPrefix) {
        // 时间戳
        LocalDateTime now = LocalDateTime.now();
        long nowSecond = now.toEpochSecond(ZoneOffset.UTC);
        long timestamp = nowSecond - BEGIN_TIMESTAMP;
        
        
        // 当前日期
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 序列号
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        
        // 拼接返回
        return (timestamp << SEQUENCE_BITS) | count;
    }
}
