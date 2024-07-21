package com.zayn.dianping.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zayn.dianping.entity.RedisData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.zayn.dianping.utils.RedisConstants.CACHE_NULL_TTL;
import static com.zayn.dianping.utils.RedisConstants.LOCK_SHOP_KEY;

/**
 * @author zayn
 * * @date 2024/7/19/下午6:21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheClient {
    private static final ExecutorService CACHE_REBUILD_POOL = Executors.newFixedThreadPool(10); // 缓存重建线程池
    private final StringRedisTemplate stringRedisTemplate;
    
    
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
    
    public void del(String key) {
        stringRedisTemplate.delete(key);
    }
    
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }
    
    /**
     * 缓存穿透解决方案
     *
     * @param keyPrefix  缓存key前缀
     * @param id         id
     * @param type       返回类型
     * @param dbFunction 数据库查询函数
     * @param time       过期时间
     * @param unit       时间单位
     * @param <R>        返回类型
     * @param <ID>       id类型
     * @return 返回结果
     */
    public <R, ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFunction, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        
        String s = this.get(key);
        
        if (StrUtil.isNotBlank(s)) {
            // 缓存命中，直接返回
            return JSONUtil.toBean(s, type);
        }
        
        if (s != null) {
            // 缓存命中，但是是空字符串
            return null;
        }
        
        R r = dbFunction.apply(id);
        if (r == null) {
            // 不存在，将空值写入redis
            this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回失败
            return null;
        }
        this.set(key, r, time, unit);
        return r;
    }
    
    /**
     * 互斥锁缓存击穿解决方案
     * 3次重试，重试等待时间加倍
     * 根据id查询商铺信息
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    public <R, ID> R queryWithMutex(String keyPrefix, String lockPrefix, ID id, Class<R> type, Function<ID, R> dbFunction, Long time, TimeUnit unit) {
        // redis key
        String key = keyPrefix + id;
        // lock key
        String lockKey = lockPrefix + id;
        
        // 从redis查询
        String s = this.get(key);
        
        // 缓存命中，直接返回
        if (StrUtil.isNotBlank(s)) {
            return JSONUtil.toBean(s, type);
        }
        
        // 要么是null，要么是""
        // 缓存命中，但是是空字符串
        if (s != null) {
            return null;
        }
        
        int retryTimes = 3; // 重试次数
        int waitTime = 50; // 重试等待时间
        
        for (int i = 0; i < retryTimes; i++) {
            // 获取互斥锁
            try {
                boolean isLock = tryLock(lockKey);
                
                // 获取锁失败，休眠重试
                if (!isLock) {
                    Thread.sleep(waitTime);
                    waitTime *= 2;
                    continue;
                }
                
                // 获取锁成功，再次查询redis
                s = this.get(key);
                if (StrUtil.isNotBlank(s)) {
                    return JSONUtil.toBean(s, type);
                }
                
                // 查询数据库
                R r = dbFunction.apply(id);
                
                // 模拟缓存重建延时
//                Thread.sleep(100);
                
                if (r == null) {
                    // 不存在，将空值写入redis
                    this.set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                    // 返回失败
                    return null;
                }
                
                // 数据库中存在，写入redis
                this.set(key, JSONUtil.toJsonStr(r), time, unit);
                
                // 返回shop
                return r;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                // 释放锁
                unlock(lockKey);
            }
        }
        // 重试次数用完，返回null
        return null;
    }
    
    /**
     * 逻辑过期解决缓存击穿
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFunction, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        
        String s = this.get(key);
        
        // 未命中，直接返回
        if (StrUtil.isBlank(s)) {
            return null;
        }
        
        // 反序列化为对象
        RedisData redisData = JSONUtil.toBean(s, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        
        LocalDateTime expireTime = redisData.getExpireTime();
        // 未过期，直接返回
        if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
        
        // 过期，异步更新缓存
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            // 二次检查
            s = this.get(key);
            if (StrUtil.isNotBlank(s)) {
                unlock(lockKey);
                return JSONUtil.toBean(s, type);
            }
            
            // 独立线程更新缓存
            CACHE_REBUILD_POOL.submit(() -> {
                try {
                    R r1 = dbFunction.apply(id);
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException("更新缓存失败");
                } finally {
                    unlock(lockKey);
                }
            });
        }
        // 返回旧数据
        return r;
    }
    
    /**
     * 释放锁
     *
     * @param lockKey 锁key
     */
    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }
    
    /**
     * 获取锁
     *
     * @param lockKey 锁key
     * @return 是否获取成功
     */
    private boolean tryLock(String lockKey) {
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }
}
