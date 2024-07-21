package com.zayn.dianping.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.entity.UserInfo;
import com.zayn.dianping.mapper.UserInfoMapper;
import com.zayn.dianping.service.IUserInfoService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 用户信息服务实现类
 * </p>
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
