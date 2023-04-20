package com.bantanger.im.service.redis;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import org.redisson.api.RedissonClient;

/**
 * Redisson 客户端管理类
 * @author BanTanger 半糖
 * @Date 2023/3/24 22:07
 */
public class RedisManager {

    private static RedissonClient redissonClient;

    public static void init(ImBootstrapConfig config) {
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
        // 初始化监听类
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(config.getIm().getLoginModel());
        userLoginMessageListener.listenerUserLogin();
    }

    public static RedissonClient getRedissonClient() {
        return redissonClient;
    }

}
