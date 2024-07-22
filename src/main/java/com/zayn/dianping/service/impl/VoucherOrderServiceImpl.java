package com.zayn.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.VoucherOrder;
import com.zayn.dianping.mapper.VoucherOrderMapper;
import com.zayn.dianping.service.ISeckillVoucherService;
import com.zayn.dianping.service.IVoucherOrderService;
import com.zayn.dianping.utils.RedisIDGenerator;
import com.zayn.dianping.utils.UserHolder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * 优惠券订单服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();
    
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("lua/seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    
    private final ISeckillVoucherService seckillVoucherService;
    private final RedisIDGenerator redisIDGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final BlockingQueue<VoucherOrder> orderTaskQueue = new ArrayBlockingQueue<>(1024 * 1024);
    private IVoucherOrderService proxy;
    
    /**
     * 抢购秒杀优惠券
     *
     * @param voucherId 优惠券id
     * @return 抢购结果
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUser().getId();
        // lua
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(), // 空key，不要传null
                String.valueOf(voucherId),
                String.valueOf(userId)
        );
        
        // 判断结果是否为0
        int status = -1;
        if (result != null) {
            status = result.intValue();
        }
        
        // 不为0，没有购买资格
        if (status != 0) {
            return Result.fail(status == 1 ? "库存不足" : "您已抢购过该优惠券");
        }
        
        // 为0，有购买资格，将下单信息加入到队列中
        long orderId = redisIDGenerator.nextId("order");
        
        // 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId); // 订单号
        voucherOrder.setVoucherId(voucherId); // 优惠券id
        voucherOrder.setUserId(userId); // 用户id
        
        // 将订单信息加入到队列中
        orderTaskQueue.add(voucherOrder);
        
        // 代理调用
        proxy = (IVoucherOrderService) AopContext.currentProxy();
        
        // 返回订单id
        return Result.ok(orderId);
    }
    
    /**
     * 创建优惠券订单
     * 一人只能抢购一张
     *
     * @param voucherOrder 订单信息
     */
    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        //  一人只能抢购一张
        Long userId = voucherOrder.getUserId(); //子线程，不能通过ThreadLocal获取
        
        int count = Math.toIntExact(query().eq("voucher_id", voucherOrder.getVoucherId())
                                           .eq("user_id", userId)
                                           .count());
        if (count > 0) {
            log.info("用户{}已抢购过该优惠券", userId);
            return;
        }
        
        // 扣减库存
        boolean success = seckillVoucherService.update().
                                               setSql("stock = stock - 1") // 扣减库存
                                               .eq("voucher_id", voucherOrder.getVoucherId()) // 优惠券id
                                               .gt("stock", 0)
                                               .update(); // 执行更新
        
        // 扣减失败
        if (!success) {
            log.info("优惠券{}库存不足", voucherOrder.getVoucherId());
            return;
        }
        
        // 保存订单
        save(voucherOrder);
    }
    
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    
    /**
     * 处理优惠券订单
     *
     * @param order 订单信息
     */
    private void handleVoucherOrder(VoucherOrder order) {
        Long userId = order.getUserId();
        // 锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if (isLock) {
            try {
                proxy.createVoucherOrder(order);
            } finally {
                lock.unlock();
            }
        }
        log.info("不能重复抢购");
    }
    
    private class VoucherOrderHandler implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // 从队列中获取订单信息
                    VoucherOrder order = orderTaskQueue.take();
                    // 创建订单
                    handleVoucherOrder(order);
                } catch (InterruptedException e) {
                    log.error("订单处理异常", e);
                }
            }
        }
    }
}
