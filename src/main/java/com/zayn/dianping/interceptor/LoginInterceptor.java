package com.zayn.dianping.interceptor;

import com.zayn.dianping.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @author zayn
 * * @date 2024/7/18/下午7:38
 */
public class LoginInterceptor implements HandlerInterceptor {
    /**
     * 在请求处理之前进行调用（Controller方法调用之前）
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }
        // 放行
        return true;
    }
}
