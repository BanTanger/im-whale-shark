package com.bantanger.im.domain.message.seq;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Redis 原子自增序列号
 * @author BanTanger 半糖
 * @Date 2023/4/7 17:22
 */
@Service
public class RedisSequence {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    public long doGetSeq(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

}
