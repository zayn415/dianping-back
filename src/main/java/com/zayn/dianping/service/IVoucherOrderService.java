package com.zayn.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.VoucherOrder;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {
    
    Result seckillVoucher(Long voucherId);
    
    void createVoucherOrder(VoucherOrder voucherOrder);
}
