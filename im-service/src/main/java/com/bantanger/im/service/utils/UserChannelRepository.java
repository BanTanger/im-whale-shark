package com.bantanger.im.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.ConnectState;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.common.model.UserSession;
import com.bantanger.im.service.redis.RedisManager;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户信息与 Channel 双向存储
 * @author BanTanger 半糖
 * @Date 2023/3/26 8:06
 */
@Slf4j
public class UserChannelRepository extends Constants {

    private static ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static Map<UserClientDto, Channel> USER_CHANNEL = new ConcurrentHashMap<>();
    private static final Object bindLocker = new Object();
    private static final Object removeLocker = new Object();

    public static void bind(UserClientDto userClientDto, Channel channel) {
        synchronized (bindLocker) {
            // 此时channel一定已经在ChannelGroup中了

            // 之前已经绑定过了，移除并释放掉之前绑定的channel
            // LoginStatusMap  userChannelKey --> channel
            if (USER_CHANNEL.containsKey(userClientDto)) {
                Channel oldChannel = USER_CHANNEL.get(userClientDto);
                CHANNEL_GROUP.remove(oldChannel);
                oldChannel.close();
            }

            // 双向绑定
            // channel -> user property
            channel.attr(AttributeKey.valueOf(ChannelConstants.UserId)).set(userClientDto.getUserId());
            channel.attr(AttributeKey.valueOf(ChannelConstants.AppId)).set(userClientDto.getAppId());
            channel.attr(AttributeKey.valueOf(ChannelConstants.ClientType)).set(userClientDto.getClientType());
            channel.attr(AttributeKey.valueOf(ChannelConstants.imei)).set(userClientDto.getImei());
            channel.attr(AttributeKey.valueOf(ChannelConstants.ClientImei)).set(userClientDto.getClientType() + ":" + userClientDto.getImei());

            // userChannelKey -> channel
            USER_CHANNEL.put(userClientDto, channel);
        }
    }

    /**
     * 从通道中获取用户信息。只要 userClientDto 和 channel 绑定中，这个方法就一定能获取的到
     * @param channel
     * @return
     */
    public static UserClientDto getUserInfo(Channel channel) {
        String userId = (String) channel.attr(AttributeKey.valueOf(ChannelConstants.UserId)).get();
        Integer appId = (Integer) channel.attr(AttributeKey.valueOf(ChannelConstants.AppId)).get();
        Integer clientType = (Integer) channel.attr(AttributeKey.valueOf(ChannelConstants.ClientType)).get();
        String imei = (String) channel.attr(AttributeKey.valueOf(ChannelConstants.imei)).get();

        return new UserClientDto(appId, userId, clientType, imei);
    }

    public static void add(Channel channel) {
        CHANNEL_GROUP.add(channel);
    }

    public static void remove(Channel channel) {
        synchronized(removeLocker) { // 确保原子性

            UserClientDto userInfo = getUserInfo(channel);

            // userInfo 有可能为空。可能 chanelActive 之后，由于前端原因（或者网络原因）没有及时绑定 userInfo。
            // 此时 netty 认为 channelInactive 了，就移除通道，这时 userInfo 就是 null
            if (ObjectUtils.isEmpty(userInfo)) {
                log.info("用户信息不存在，请检查");
                return ;
            }
            // TODO 延迟双删：等待数据包传输完再删除 channel
            USER_CHANNEL.remove(userInfo);
            CHANNEL_GROUP.remove(channel);

            // Redis 删除用户 Session
            removeSession(userInfo);

            // 关闭channel
            channel.close();
        }
    }

    public static void remove(UserClientDto userClientDto) {
        // 确保原子性
        synchronized(removeLocker) {

            Channel channel = USER_CHANNEL.get(userClientDto);
            USER_CHANNEL.remove(userClientDto);
            CHANNEL_GROUP.remove(channel);

            // 关闭channel
            if (!ObjectUtils.isEmpty(channel)) {
                channel.close();
            }
        }
    }

    private static void removeSession(UserClientDto userInfo) {
        RedissonClient redissonClient = RedisManager.getRedissonClient();
        RMap<String, String> map = redissonClient.getMap(userInfo.getAppId() + RedisConstants.UserSessionConstants + userInfo.getUserId());
        // 删除 Hash 里的 key：clientType:imei，key 存放用户的 Session
        map.remove(userInfo.getClientType() + ":" + userInfo.getImei());
    }

    /**
     * 判断用户是否在线
     * LoginStatusMap 和 channelGroup 中均能找得到对应的 channel 说明用户在线
     * @return      在线就返回对应的channel，不在线返回null
     */
    public static Channel isBind(UserClientDto userClientDto) {
        Channel channel = USER_CHANNEL.get(userClientDto);
        if (ObjectUtils.isEmpty(channel)) {
            return null;
        }
        return CHANNEL_GROUP.find(channel.id());
    }

    public static boolean isBind(Channel channel) {
        UserClientDto userInfo = getUserInfo(channel);
        return !ObjectUtils.isEmpty(userInfo) &&
                !ObjectUtils.isEmpty(USER_CHANNEL.get(userInfo));
    }

    public static void forceOffLine(UserClientDto userClientDto) {
        Channel channel = isBind(userClientDto);
        if (!ObjectUtils.isEmpty(channel)) {
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(userClientDto.getAppId() + RedisConstants.UserSessionConstants + userClientDto.getUserId());
            String key = userClientDto.getClientType() + ":" + userClientDto.getImei();
            String userSessionValue = map.get(key);

            if (!StringUtils.isBlank(userSessionValue)) {
                UserSession userSession = JSONObject.parseObject(userSessionValue, UserSession.class);
                userSession.setConnectState(ConnectState.CONNECT_STATE_OFFLINE.getCode());
                map.put(key, JSONObject.toJSONString(userSession));
            }
            // 移除通道。服务端单方面关闭连接。前端心跳会发送失败
            remove(userClientDto);
        }
    }

    public static void forceOffLine(Channel channel) {
        UserClientDto userInfo = getUserInfo(channel);
        try {
            forceOffLine(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 遍历某用户绑定的所有 Channel
     * @param appId
     * @param userId
     * @return
     */
    public static List<Channel> getUserChannels(Integer appId, String userId) {
        Set<UserClientDto> channelInfos = USER_CHANNEL.keySet();
        List<Channel> channels = new ArrayList<>();

        channelInfos.forEach(channel -> {
            if (appId.equals(channel.getAppId()) && userId.equals(channel.getUserId())) {
                channels.add(USER_CHANNEL.get(channel));
            }
        });
        return channels;
    }

    public static Channel getUserChannel(Integer appId, String userId, Integer clientType, String imei) {
        UserClientDto dto = new UserClientDto();
        dto.setUserId(userId);
        dto.setAppId(appId);
        dto.setClientType(clientType);
        dto.setImei(imei);
        if (!USER_CHANNEL.containsKey(dto)) {
            log.error("channel 通道 没有 [{}] 信息", JSONObject.toJSONString(dto));
            return null;
        }
        return USER_CHANNEL.get(dto);
    }

    public synchronized static void print() {
        log.info("所有通道的长id：");
        for (Channel channel : CHANNEL_GROUP) {
            log.info(channel.id().asLongText());
        }
        log.info("userId -> channel 的映射：");
        for (Map.Entry<UserClientDto, Channel> entry : USER_CHANNEL.entrySet()) {
            log.info("userId: {} ---> channelId: {}", entry.getKey(), entry.getValue().id().asLongText());
        }
    }

}
