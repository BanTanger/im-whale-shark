package com.bantanger.im.service.utils;

import com.alibaba.fastjson2.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.connect.ImSystemConnectState;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.redis.RedisManager;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 20:51
 */
public class SessionSocketHolder {

    private static final Map<UserClientDto, NioSocketChannel> CHANNELS = new ConcurrentHashMap<>();
    private UserClientDto userClientDto = new UserClientDto();

    public static void put(UserClientDto userClientDto, NioSocketChannel channel) {
        CHANNELS.put(userClientDto, channel);
    }

    public static NioSocketChannel get(UserClientDto userClientDto) {
        return CHANNELS.get(userClientDto);
    }

    public static void remove(UserClientDto userClientDto) {
        CHANNELS.remove(userClientDto);
    }

    public static void remove(NioSocketChannel channel) {
        CHANNELS.entrySet().stream().filter(entitiy -> entitiy.getValue() == channel)
                .forEach(entry -> CHANNELS.remove(entry.getKey()));
    }

    /**
     * 用户登出(离线)
     * @param nioSocketChannel
     */
    public static void removeUserSession(NioSocketChannel nioSocketChannel) {
        // 删除 Session and redisson 里的 session
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.ClientType)).get();

        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);

        SessionSocketHolder.remove(userClientDto);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        // 删除 Hash 里的 key，key 存放用户的 Session
        map.remove(clientType.toString());
        nioSocketChannel.close();
    }

    /**
     * 用户退后台
     * @param nioSocketChannel
     */
    public static void offlineUserSession(NioSocketChannel nioSocketChannel) {
        // 删除 Session and redisson 里的 session
        String userId = (String) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.UserId)).get();
        Integer appId = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.AppId)).get();
        Integer clientType = (Integer) nioSocketChannel.attr(AttributeKey.valueOf(Constants.ChannelConstants.ClientType)).get();

        UserClientDto userClientDto = new UserClientDto();
        userClientDto.setUserId(userId);
        userClientDto.setAppId(appId);
        userClientDto.setClientType(clientType);

        SessionSocketHolder.remove(userClientDto);
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(appId + Constants.RedisConstants.UserSessionConstants + userId);
        String sessionStr = map.get(clientType.toString());

        if (!StringUtils.isBlank(sessionStr)) {
            UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
            userSession.setConnectState(ImSystemConnectState.CONNECT_STATE_OFFLINE.getCode());
            map.put(clientType.toString(), JSONObject.toJSONString(userSession));
        }
        nioSocketChannel.close();
    }

}
