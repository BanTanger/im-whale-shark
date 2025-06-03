package com.bantanger.im.infrastructure.support.ids;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @author chensongmin
 * @description 基于 Redis 的序列号 ID 生成器
 * @date 2025/6/3
 */
@Component
public class SequenceIdWorker {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    public long doGetSeq(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

}
