package com.bantanger.im.common.constant;

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

        /**
         * channel 绑定的 imei 号，标识用户登录设备号
         */
        public static final String imei = "imei";

        /**
         * channel 绑定的 clientType 和 imei Key
         */
        public static final String ClientImei = "clientImei";
    }

    public static class RedisConstants {
        /**
         * UserSign，格式：appId:UserSign:
         */
        public static final String UserSign = ":userSign:";
        /**
         * 用户登录端消息通道信息
         */
        public static final String UserLoginChannel = "signal/channel/LOGIN_USER_INNER_QUEUE";
        /**
         * 用户session：格式为 appId + userSessionConstants + 用户 ID
         * 例如：10001:userSessionConstants:userId
         */
        public static final String UserSessionConstants = ":userSession:";
        /**
         * 缓存客户端消息防重，格式： appId + :cacheMessage: + messageId
         */
        public static final String CacheMessage = ":cacheMessage:";
        /**
         * 缓存离线消息 获取用户消息队列 格式：appId + :offlineMessage: + fromId / toId
         */
        public static final String OfflineMessage = ":offlineMessage:";
        /**
         * 缓存群组成员列表
         */
        public static final String GroupMembers = ":groupMembers:";
        /**
         * 用户所有模块的偏序前缀
         */
        public static final String SeqPrefix = ":seq:";
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

    public static class CallbackCommand{

        public static final String ModifyUserAfter = "user.modify.after";

        public static final String CreateGroupAfter = "group.create.after";

        public static final String UpdateGroupAfter = "group.update.after";

        public static final String DestoryGroupAfter = "group.destory.after";

        public static final String TransferGroupAfter = "group.transfer.after";

        public static final String GroupMemberAddBefore = "group.member.add.before";

        public static final String GroupMemberAddAfter = "group.member.add.after";

        public static final String GroupMemberDeleteAfter = "group.member.delete.after";

        public static final String AddFriendBefore = "friend.add.before";

        public static final String AddFriendAfter = "friend.add.after";

        public static final String UpdateFriendBefore = "friend.update.before";

        public static final String UpdateFriendAfter = "friend.update.after";

        public static final String DeleteFriendAfter = "friend.delete.after";

        public static final String AddBlackAfter = "black.add.after";

        public static final String DeleteBlack = "black.delete";

        public static final String SendMessageAfter = "message.send.after";

        public static final String SendMessageBefore = "message.send.before";

    }

    public static class SeqConstants {
        // 保证消息有序性 Key
        /** 单聊消息有序 */
        public static final String MessageSeq = ":messageSeq:";
        /** 群聊消息有序 */
        public static final String GroupMessageSeq = ":groupMessageSeq:";
        /** 会话消息有序 */
        public static final String ConversationSeq = "conversationSeq";

        // 用于消息数据同步 Key
        /** 好友数量记录 */
        public static final String FriendShipSeq = "friendShipSeq";
        /** 好友申请记录 */
        public static final String FriendShipRequestSeq = "friendShipRequestSeq";
        /** 好友标签记录 */
        public static final String FriendShipGroupSeq = "friendShipGroupSeq";
        /** 群聊数量记录 */
        public static final String GroupSeq = "groupSeq";

    }

}
