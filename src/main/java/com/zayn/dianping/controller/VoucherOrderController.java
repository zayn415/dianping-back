package com.zayn.dianping.controller;


import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.service.IVoucherOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优惠券控制器
 */
@RestController
@RequestMapping("/voucher-order")
@RequiredArgsConstructor
public class VoucherOrderController {
    
    private final IVoucherOrderService voucherOrderService;
    
    /**
     * 抢购秒杀优惠券
     *
     * @param voucherId 优惠券id
     * @return 抢购结果
     */
    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {
        return voucherOrderService.seckillVoucher(voucherId);
    }
}
