package com.zayn.dianping.service.impl;


import cn.hutool.core.lang.TypeReference;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.Shop;
import com.zayn.dianping.mapper.ShopMapper;
import com.zayn.dianping.service.IShopService;
import com.zayn.dianping.utils.CacheClient;
import com.zayn.dianping.utils.SystemConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.zayn.dianping.utils.RedisConstants.*;

/**
 * 服务实现类
 */
@Service
@RequiredArgsConstructor
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    
    private final CacheClient cacheClient;
    
    /**
     * 根据id查询商铺信息
     * 添加超时剔除和主动更新
     * 加锁防止缓存击穿
     *
     * @param id 商铺id
     * @return 商铺详情数据
     */
    @Override
    public Result queryById(Long id) {
        // 查询redis
        
        // 缓存穿透
//        Shop shop
//                = cacheClient.queryWithPassThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        
        // 互斥锁解决缓存击穿方案
        Shop shop
                = cacheClient.queryWithMutex(CACHE_SHOP_KEY, LOCK_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.MINUTES);
        
        // 逻辑过期解决缓存击穿方案
//        Shop shop = cacheClient.queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, 20L, TimeUnit.SECONDS);
        
        // 查询失败
        if (shop == null) {
            return Result.fail("商铺不存在");
        }
        
        // 查询成功
        return Result.ok(shop);
    }
    
    /**
     * 更新商铺信息
     *
     * @param shop 商铺数据
     * @return 更新后的商铺数据
     */
    @Override
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("商铺id不能为空");
        }
        
        // 更新数据库
        updateById(shop);
        
        // 删除redis缓存
        cacheClient.del(CACHE_SHOP_KEY + id);
        
        // 返回成功
        return Result.ok();
    }
    
    /**
     * 根据商铺类型分页查询商铺信息
     * 添加redis缓存
     * 800ms -> 80ms
     * todo 抽取以及优化穿透、击穿、雪崩解决方案
     *
     * @param typeId  商铺类型
     * @param current 页码
     * @return 商铺列表
     */
    @Override
    public Result queryPage(Integer typeId, Integer current) {
        String cacheKey = CACHE_SHOP_TYPE_KEY + typeId + ":" + current;
        
        // 查询缓存
        String cachedPage = cacheClient.get(cacheKey);
        // 命中缓存
        if (cachedPage != null) {
            Page<Shop> page = JSONUtil.toBean(cachedPage, new TypeReference<Page<Shop>>() {
            }, true);
            return Result.ok(page.getRecords());
        }
        
        // 查询数据库
        Page<Shop> page = query()
                .eq("type_id", typeId)
                .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
        
        cacheClient.set(cacheKey, JSONUtil.toJsonStr(page), CACHE_SHOP_TYPE_TTL, TimeUnit.MINUTES);
        
        return Result.ok(page.getRecords());
    }
}
