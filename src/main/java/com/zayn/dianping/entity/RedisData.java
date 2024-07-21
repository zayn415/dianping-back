package com.zayn.dianping.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author zayn
 * * @date 2024/7/19/下午5:31
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
