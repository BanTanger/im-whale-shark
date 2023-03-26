package com.bantanger.im.service.utils;

import com.alibaba.fastjson2.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.connect.ImSystemConnectState;
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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 8:06
 */
@Slf4j
public class UserChannelRepository extends Constants {

    private static ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static Map<String, Channel> USER_CHANNEL = new ConcurrentHashMap<>();
    private static final Object bindLocker = new Object();
    private static final Object removeLocker = new Object();

    public static void bind(String userChannelKey, Channel channel) {
        synchronized (bindLocker) {
            // 此时channel一定已经在ChannelGroup中了

            // 之前已经绑定过了，移除并释放掉之前绑定的channel
            // map  userChannelKey --> channel
            if (USER_CHANNEL.containsKey(userChannelKey)) {
                Channel oldChannel = USER_CHANNEL.get(userChannelKey);
                CHANNEL_GROUP.remove(oldChannel);
                oldChannel.close();
            }

            // 双向绑定
            // channel -> userChannelKey
            AttributeKey<String> key = AttributeKey.valueOf(ChannelConstants.UserChannelKey);
            channel.attr(key).set(userChannelKey);

            // userChannelKey -> channel
            USER_CHANNEL.put(userChannelKey, channel);
        }
    }

    /**
     * 从通道中获取userId。只要userId和channel绑定周，这个方法就一定能获取的到
     * @param channel
     * @return
     */
    public static String getUserChannelKey(Channel channel) {
        AttributeKey<String> key = AttributeKey.valueOf(ChannelConstants.UserChannelKey);
        return channel.attr(key).get();
    }

    public static void add(Channel channel) {
        CHANNEL_GROUP.add(channel);
    }

    public static void remove(Channel channel) {
        synchronized(removeLocker) { // 确保原子性

            String userChannelKey = getUserChannelKey(channel);

            // userChannelKey 有可能为空。可能 chanelActive 之后，由于前端原因（或者网络原因）没有及时绑定 userChannelKey。
            // 此时 netty 认为 channelInactive 了，就移除通道，这时 userChannelKey 就是 null
            if (!StringUtils.isEmpty(userChannelKey)) {
                USER_CHANNEL.remove(userChannelKey);
            }

            CHANNEL_GROUP.remove(channel);

            // 预处理 userChannelKey
            String[] split = userChannelKey.split(":");
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(split[1] + RedisConstants.UserSessionConstants + split[0]);
            // 删除 Hash 里的 key，key 存放用户的 Session
            map.remove(split[2]);

            // 关闭channel
            channel.close();
        }
    }

    public static void remove(String userChannelKey) {
        // 确保原子性
        synchronized(removeLocker) {

            Channel channel = USER_CHANNEL.get(userChannelKey);
            USER_CHANNEL.remove(userChannelKey);
            CHANNEL_GROUP.remove(channel);

            // 关闭channel
            if (!ObjectUtils.isEmpty(channel)) {
                channel.close();
            }
        }
    }

    /**
     * 判断用户是否在线
     * map 和 channelGroup 中均能找得到对应的 channel 说明用户在线
     * @return      在线就返回对应的channel，不在线返回null
     */
    public static Channel isBind(String userChannelKey) {
        Channel channel = USER_CHANNEL.get(userChannelKey);
        if (ObjectUtils.isEmpty(channel)) {
            return null;
        }
        return CHANNEL_GROUP.find(channel.id());
    }

    public static boolean isBind(Channel channel) {
        AttributeKey<String> key = AttributeKey.valueOf(ChannelConstants.UserChannelKey);
        String userChannelKey = channel.attr(key).get();
        return !ObjectUtils.isEmpty(userChannelKey) &&
                !ObjectUtils.isEmpty(USER_CHANNEL.get(userChannelKey));
    }

    public static void forceOffLine(String userChannelKey) {
        Channel channel = isBind(userChannelKey);
        String[] split = userChannelKey.split(":");
        if (!ObjectUtils.isEmpty(channel)) {
            RedissonClient redissonClient = RedisManager.getRedissonClient();
            RMap<String, String> map = redissonClient.getMap(split[1] + RedisConstants.UserSessionConstants + split[0]);
            String sessionStr = map.get(split[2]);

            if (!StringUtils.isBlank(sessionStr)) {
                UserSession userSession = JSONObject.parseObject(sessionStr, UserSession.class);
                userSession.setConnectState(ImSystemConnectState.CONNECT_STATE_OFFLINE.getCode());
                map.put(split[2], JSONObject.toJSONString(userSession));
            }
            // 移除通道。服务端单方面关闭连接。前端心跳会发送失败
            remove(userChannelKey);
        }
    }

    public static void forceOffLine(Channel channel) {
        AttributeKey<String> key = AttributeKey.valueOf(ChannelConstants.UserChannelKey);
        String userChannelKey = channel.attr(key).get();
        try {
            forceOffLine(userChannelKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
//
//    /**
//     * 消息推送
//     * @param receiverId
//     * @param msgModel
//     */
//    public static void pushMsg(String receiverId, MsgModel msgModel) {
//        Channel receiverChannel = isBind(receiverId);
//        if (!ObjectUtils.isEmpty(receiverChannel)) {
//            TextWebSocketFrame frame = new TextWebSocketFrame(toJson(msgModel));
//            receiverChannel.writeAndFlush(frame);
//        } else {
//            // 离线状态
//            log.info("{} 用户离线", receiverId);
//        }
//    }
//
//    private static String toJson(MsgModel msgModel) {
//        // 在线，就推送；离线，不做处理
//        ObjectMapper mapper = SpringUtils.getBean(ObjectMapper.class);
//        try {
//            return mapper.writeValueAsString(msgModel);
//        } catch (JsonProcessingException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    public synchronized static void print() {
        log.info("所有通道的长id：");
        for (Channel channel : CHANNEL_GROUP) {
            log.info(channel.id().asLongText());
        }
        log.info("userId -> channel 的映射：");
        for (Map.Entry<String, Channel> entry : USER_CHANNEL.entrySet()) {
            log.info("userId: {} ---> channelId: {}", entry.getKey(), entry.getValue().id().asLongText());
        }
    }

    public static String parseUserClientDto(Object obj) {
        StringBuffer sb = new StringBuffer();
        Class clazz=obj.getClass();//获得实体类名
        Field[] fields = obj.getClass().getDeclaredFields();//获得属性
        //获得Object对象中的所有方法
        for(Field field:fields){
            try {
                PropertyDescriptor pd = new PropertyDescriptor(field.getName(), clazz);
                Method getMethod = pd.getReadMethod();//获得get方法
                Object s = getMethod.invoke(obj);//此处为执行该Object对象的get方法
                sb.append(s).append(":");
            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return sb.toString().substring(0, sb.length() - 1);
    }

}
