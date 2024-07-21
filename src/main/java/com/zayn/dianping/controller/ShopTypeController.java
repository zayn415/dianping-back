package com.zayn.dianping.controller;


import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.service.IShopTypeService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商铺类型控制器
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;
    
    @GetMapping("list")
    public Result queryTypeList() {
        return typeService.getTypeList();
    }
}
