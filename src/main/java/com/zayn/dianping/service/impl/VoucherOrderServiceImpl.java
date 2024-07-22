package com.zayn.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.SeckillVoucher;
import com.zayn.dianping.entity.VoucherOrder;
import com.zayn.dianping.mapper.VoucherOrderMapper;
import com.zayn.dianping.service.ISeckillVoucherService;
import com.zayn.dianping.service.IVoucherOrderService;
import com.zayn.dianping.utils.RedisIDGenerator;
import com.zayn.dianping.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * <p>
 * 优惠券订单服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    private final ISeckillVoucherService seckillVoucherService;
    private final RedisIDGenerator redisIDGenerator;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    
    /**
     * 抢购秒杀优惠券
     *
     * @param voucherId 优惠券id
     * @return 抢购结果
     */
    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        
        // 判断是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀未开始");
        }
        
        // 判断是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已结束");
        }
        
        // 判断是否有库存
        if (voucher.getStock() <= 0) {
            return Result.fail("库存不足");
        }
        
        // aop代理对象
        // spring事务失效
        // synchronized锁，只能保证单进程下的线程安全
        // 事务提交后才释放锁
        // todo 集群下不安全
        Long userId = UserHolder.getUser().getId();
        // 分布式锁
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean locked = lock.tryLock();
        if (!locked) {
            return Result.fail("请勿重复抢购");
        }
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * 创建优惠券订单
     *
     * @param voucherId 优惠券id
     * @return 订单id
     */
    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // todo 一人只能抢购一张
        Long userId = UserHolder.getUser().getId();
        
        int count = Math.toIntExact(query().eq("voucher_id", voucherId)
                                           .eq("user_id", userId)
                                           .count());
        if (count > 0) {
            return Result.fail("您已抢购过该优惠券");
        }
        
        // 扣减库存
        boolean success = seckillVoucherService.update().
                                               setSql("stock = stock - 1") // 扣减库存
                                               .eq("voucher_id", voucherId) // 优惠券id
//                                               .eq("stock", voucher.getStock()) // 防止超卖
                                               .gt("stock", 0) // 防止超卖
                                               .update(); // 执行更新
        
        // 扣减失败
        if (!success) {
            return Result.fail("库存不足");
        }
        
        // 生成订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIDGenerator.nextId("order");
        voucherOrder.setId(orderId); // 订单号
        voucherOrder.setVoucherId(voucherId); // 优惠券id
        voucherOrder.setUserId(userId); // 用户id
        
        save(voucherOrder);
        
        // 返回结果
        return Result.ok(orderId);
    }
}
