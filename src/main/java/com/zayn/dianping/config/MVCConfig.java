package com.zayn.dianping.config;

import com.zayn.dianping.interceptor.LoginInterceptor;
import com.zayn.dianping.interceptor.RefreshTokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zayn
 * * @date 2024/7/18/下午7:45
 */
@Configuration
@RequiredArgsConstructor
public class MVCConfig implements WebMvcConfigurer {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        registry.addInterceptor(new LoginInterceptor())
                // 放行
                .excludePathPatterns(
                        "shop/**",
                        "voucher/**",
                        "shop-type/**",
                        "blog/hot",
                        "/user/code",
                        "/user/login"
                )
                .order(1);
    }
}
