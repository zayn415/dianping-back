package com.zayn.dianping.utils;

/**
 * 锁接口
 *
 * @author zayn
 * * @date 2024/7/21/下午10:01
 */

public interface ILock {
    /**
     * 加锁
     *
     * @param timeSeconds 锁的时间
     * @return 是否加锁成功
     */
    boolean lock(long timeSeconds);
    
    /**
     * 解锁
     */
    void unlock();
}
