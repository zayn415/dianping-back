package com.zayn.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.Voucher;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IVoucherService extends IService<Voucher> {
    
    Result queryVoucherOfShop(Long shopId);
    
    void addSeckillVoucher(Voucher voucher);
}
