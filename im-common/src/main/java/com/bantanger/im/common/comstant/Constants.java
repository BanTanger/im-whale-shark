package com.bantanger.im.common.comstant;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 22:55
 */
public class Constants {

    public static class ChannelConstants {
        /**
         * channel 绑定的 userId Key
         */
        public static final String UserId = "userId";
        /**
         * channel 绑定的 appId Key
         */
        public static final String AppId = "appId";
        /**
         * channel 绑定的端类型
         */
        public static final String ClientType = "clientType";
        /**
         * channel 绑定的读写时间
         */
        public static final String ReadTime = "readTime";

        public static final String UserChannelKey = AppId + ":" + UserId + ":" + ClientType;
    }

    public static class RedisConstants {
        /**
         * 用户session：格式为 appId + userSessionConstants + 用户 ID
         * 例如：10001:userSessionConstants:userId
         */
        public static final String UserSessionConstants = ":userSession:";
    }

    public static class RabbitmqConstants {

        public static final String Im2UserService = "pipeline2UserService";

        public static final String Im2MessageService = "pipeline2MessageService";

        public static final String Im2GroupService = "pipeline2GroupService";

        public static final String Im2FriendshipService = "pipeline2FriendshipService";

        public static final String MessageService2Im = "messageService2Pipeline";

        public static final String GroupService2Im = "GroupService2Pipeline";

        public static final String FriendShip2Im = "friendShip2Pipeline";

        public static final String StoreP2PMessage = "storeP2PMessage";

        public static final String StoreGroupMessage = "storeGroupMessage";

    }

    public static class ZkConstants {

        public static final String ImCoreZkRoot = "/im-coreRoot";

        public static final String ImCoreZkRootTcp = "/tcp";

        public static final String ImCoreZkRootWeb = "/web";
    }
}
