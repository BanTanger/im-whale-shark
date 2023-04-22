package com.bantanger.im.service.utils;

import com.bantanger.im.common.constant.Constants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/9 14:16
 */
@Service
public class UserSequenceRepository {

    @Resource
    RedisTemplate redisTemplate;

    /**
     * 记录用户所有模块: 好友、群组、会话的数据偏序
     * Redis Hash 记录
     * uid 做 key, 具体 seq 做 value
     * @param appId
     * @param userId
     * @param type
     * @param seq
     */
    public void writeUserSeq(Integer appId, String userId, String type, Long seq) {
        String key = appId + Constants.RedisConstants.SeqPrefix + userId;
        redisTemplate.opsForHash().put(key, type, seq);
    }

}
