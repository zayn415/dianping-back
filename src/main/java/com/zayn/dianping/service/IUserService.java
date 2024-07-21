package com.zayn.dianping.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.zayn.dianping.domain.dto.LoginFormDTO;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.entity.User;
import jakarta.servlet.http.HttpSession;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IUserService extends IService<User> {
    
    Result sendCode(String phone, HttpSession session);
    
    Result login(LoginFormDTO loginForm, HttpSession session);
}
