package com.zayn.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.Shop;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IShopService extends IService<Shop> {
    
    Result queryById(Long id);
    
    Result update(Shop shop);
    
    Result queryPage(Integer typeId, Integer current);
}
