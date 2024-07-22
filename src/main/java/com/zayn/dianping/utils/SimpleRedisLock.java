package com.zayn.dianping.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * @author zayn
 * * @date 2024/7/21/下午10:02
 */
public class SimpleRedisLock implements ILock {
    private static final String LOCK_PREFIX = "lock:";
    private static final DefaultRedisScript<Long> UNLOCK_LUA;
    
    static {
        UNLOCK_LUA = new DefaultRedisScript<>();
        UNLOCK_LUA.setLocation(new ClassPathResource("lua/unlock.lua"));
        UNLOCK_LUA.setResultType(Long.class);
    }
    
    private final StringRedisTemplate stringRedisTemplate;
    private final String name;
    private final String THREAD_PREFIX = UUID.randomUUID().toString(true) + ":";
    
    public SimpleRedisLock(StringRedisTemplate stringRedisTemplate, String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }
    
    /**
     * 加锁
     * 记录线程标识，防止其他线程解锁
     *
     * @param timeSeconds 锁的时间
     * @return 是否加锁成功
     */
    @Override
    public boolean lock(long timeSeconds) {
        // 线程标识
        String threadId = THREAD_PREFIX + Thread.currentThread().threadId();
        Boolean isLock = stringRedisTemplate.opsForValue()
                                            .setIfAbsent(LOCK_PREFIX + name, threadId, timeSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(isLock);
    }
    
    /**
     * 解锁
     */
    @Override
    public void unlock() {
        stringRedisTemplate.execute(
                UNLOCK_LUA,
                Collections.singletonList(LOCK_PREFIX + name),
                THREAD_PREFIX + Thread.currentThread().threadId()
        );
    }
    
    /**
     * 解锁
     * 判断线程标识，防止其他线程解锁
     * 判断锁和删除锁之间发生阻塞也会导致误删 -> 使用lua脚本，确保原子性
     */
/*    @Override
    public void unlock() {
        String threadId = THREAD_PREFIX + Thread.currentThread().threadId();
        String id = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
        if (!threadId.equals(id)) {
            stringRedisTemplate.delete(LOCK_PREFIX + name);
        }
    }*/
}
