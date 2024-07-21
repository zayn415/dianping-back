package com.zayn.dianping.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.ShopType;
import com.zayn.dianping.mapper.ShopTypeMapper;
import com.zayn.dianping.service.IShopTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zayn.dianping.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 获取商铺类型列表
     *
     * @return 商铺类型列表
     */
    @Override
    public Result getTypeList() {
        // 从redis查询
        List<String> shopTypeList = stringRedisTemplate.opsForList().range("shop-type", 0, -1);
        
        // 存在，直接返回
        if (shopTypeList != null && !shopTypeList.isEmpty()) {
            List<ShopType> list = shopTypeList.stream()
                                              .map(item -> JSONUtil.toBean(item, ShopType.class))
                                              .collect(Collectors.toList());
            return Result.ok(list);
        }
        
        // 不存在，查询数据库
        List<ShopType> list = list();
        if (list == null || list.isEmpty()) {
            return Result.fail("商铺类型不存在");
        }
        
        // 写入到 Redis
        List<String> jsonList = list.stream()
                                    .map(JSONUtil::toJsonStr)
                                    .collect(Collectors.toList());
        stringRedisTemplate.opsForList().rightPushAll("shop-type", jsonList);
        
        // 设置过期时间24小时
        stringRedisTemplate.expire("shop-type", CACHE_SHOP_TTL, TimeUnit.DAYS);
        
        return Result.ok(list);
    }
}
