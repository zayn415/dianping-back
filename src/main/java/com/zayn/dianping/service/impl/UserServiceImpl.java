package com.zayn.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zayn.dianping.domain.dto.LoginFormDTO;
import com.zayn.dianping.domain.dto.Result;
import com.zayn.dianping.domain.dto.UserDTO;
import com.zayn.dianping.entity.User;
import com.zayn.dianping.mapper.UserMapper;
import com.zayn.dianping.service.IUserService;
import com.zayn.dianping.utils.RegexUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.zayn.dianping.utils.RedisConstants.*;
import static com.zayn.dianping.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    
    private final StringRedisTemplate stringRedisTemplate;
    
    /**
     * 发送手机验证码
     *
     * @param phone   手机号
     * @param session session
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 1. 校验手机号
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号格式不正确
            log.debug("手机号格式不正确：{}", phone);
            return Result.fail("手机号格式不正确");
        }
        
        // 2. 生成验证码
        String code = RandomUtil.randomNumbers(6); // 生成6位随机数字
        
        // 3. 保存验证码到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        
        
        // 4. 发送验证码
        // todo 接入短信服务商发送验证码
        log.debug("手机号：{}，验证码：{}", phone, code);
        
        // 5. 返回ok
        return Result.ok();
    }
    
    /**
     * 登录功能
     *
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     * @param session   session
     * @return 登录结果
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 1. 校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            // 手机号格式不正确
            log.error("手机号格式不正确：{}", phone);
            return Result.fail("手机号格式不正确");
        }
        
        // 2. 校验验证码
        String code = loginForm.getCode();
        
        // 从redis中获取保存的验证码
        String redisCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        
        // 判断验证码是否正确
        if (!code.equals(redisCode)) {
            // 验证码不正确
            log.error("验证码不正确：{}，正确验证码：{}", code, redisCode);
            return Result.fail("验证码不正确");
        }
        
        User user = query().eq("phone", phone).one();
        
        if (user == null) {
            // 用户不存在，注册用户
            log.error("用户不存在，注册用户：{}", phone);
            user = createUserByPhone(phone);
        }
        
        // 保存用户信息到redis
        String token = UUID.randomUUID().toString(true); // 生成token，todo 完善安全性
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class); // 将user对象转为UserDTO对象
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                           .setIgnoreNullValue(true)
                           .setFieldValueEditor((field, value) -> value.toString())); // 将对象转为map
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap); // 保存用户信息
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.SECONDS); // 设置过期时间
        
        // 返回json 的 token
        return Result.ok(JSONUtil.createObj().set("token", token));
    }
    
    private User createUserByPhone(String phone) {
        User user = new User();
        user.setPhone(phone)
            .setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
        save(user);
        return user;
    }
}
