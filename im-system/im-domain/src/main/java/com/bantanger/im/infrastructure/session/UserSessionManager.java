package com.bantanger.im.infrastructure.session;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.device.ConnectStatusEnum;
import com.bantanger.im.common.model.UserSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 获取用户所有 Channel 里的 Session
 * @author BanTanger 半糖
 * @Date 2023/3/31 20:12
 */
@Component
public class UserSessionManager implements IUserSessionManager {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public List<UserSession> getUserSession(Integer appId, String userId) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(userSessionKey);

        return entries.values().stream()
                .map(Object::toString)
                .map(value -> JSONObject.parseObject(value, UserSession.class))
                .filter(userSession -> ConnectStatusEnum.ONLINE_STATUS.getCode().equals(userSession.getConnectState()))
                .collect(Collectors.toList());
    }

    @Override
    public UserSession getUserSession(Integer appId, String userId, Integer clientType, String imei) {
        String userSessionKey = appId + Constants.RedisConstants.UserSessionConstants + userId;
        String hashKey = clientType + ":" + imei;
        // 通过 userSessionKey 获取用户的 Session map 集合，再通过 hashKey 键值寻找到指定的端 Session value 值
        Object value = stringRedisTemplate.opsForHash().get(userSessionKey, hashKey);
        assert value != null;
        UserSession userSession = JSONObject.parseObject(value.toString(), UserSession.class);
        return userSession;
    }

}
