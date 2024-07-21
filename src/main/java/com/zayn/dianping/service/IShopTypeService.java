package com.zayn.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.ShopType;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IShopTypeService extends IService<ShopType> {
    
    Result getTypeList();
}
