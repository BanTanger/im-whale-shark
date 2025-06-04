package com.bantanger.im.common.redis;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import lombok.Getter;
import org.redisson.api.RedissonClient;

/**
 * Redisson 客户端管理类
 * @author BanTanger 半糖
 * @Date 2023/3/24 22:07
 */
public class RedissonManager {

    @Getter
    private static RedissonClient redissonClient;

    public static void init(ImBootstrapConfig config) {
        SingleClientStrategy singleClientStrategy = new SingleClientStrategy();
        redissonClient = singleClientStrategy.getRedissonClient(config.getIm().getRedis());
        // 初始化监听类
        UserLoginMessageListener userLoginMessageListener = new UserLoginMessageListener(config.getIm().getLoginModel());
        userLoginMessageListener.listenerUserLogin();
    }

}
