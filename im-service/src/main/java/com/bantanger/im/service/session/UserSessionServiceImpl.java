package com.bantanger.im.service.session;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.ConnectStatusEnum;
import com.bantanger.im.common.model.UserSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 获取用户所有 Channel 里的 Session
 * @author BanTanger 半糖
 * @Date 2023/3/31 20:12
 */
@Component
public class UserSessionServiceImpl implements UserSessionService {

    @Resource
    StringRedisTemplate redisTemplate;

    @Override
    public List<UserSession> getUserSession(Integer appId, String userId) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(userSessionKey);

        return entries.values().stream()
                .map(Object::toString)
                .map(value -> JSONObject.parseObject(value, UserSession.class))
                .filter(userSession -> ConnectStatusEnum.ONLINE_STATUS.getCode()
                        .equals(userSession.getConnectState()))
                .collect(Collectors.toList());
    }

    @Override
    public UserSession getUserSession(Integer appId, String userId, Integer clientType, String imei) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        String hashKey = clientType + imei;
        // 通过 userSessionKey 获取用户的 Session map 集合，再通过 hashKey 键值寻找到指定的端 Session value 值
        Object value = redisTemplate.opsForHash().get(userSessionKey, hashKey);
        assert value != null;
        UserSession userSession = JSONObject.parseObject(value.toString(), UserSession.class);
        return userSession;
    }

}
