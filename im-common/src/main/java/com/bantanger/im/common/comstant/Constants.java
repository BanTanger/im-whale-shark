package com.bantanger.im.common.comstant;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 22:55
 */
public class Constants {

    public static class RedisConstants {
        /**
         * 用户session：格式为 appId + userSessionConstants + 用户 ID
         * 例如：10001:userSessionConstants:userId
         */
        public static final String UserSessionConstants = ":userSession:";
    }
}
