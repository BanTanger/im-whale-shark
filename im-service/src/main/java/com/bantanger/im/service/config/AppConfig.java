package com.bantanger.im.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 8:57
 */
@Data
@Component
@ConfigurationProperties(prefix = "appconfig")
public class AppConfig {

    private String privateKey;

    /**
     * zk 连接地址
     */
    private String zkAddr;

    /**
     * zk 最大超时时长
     */
    private Integer zkConnectTimeOut;

    /**
     * im 管道路由策略
     */
    private Integer imRouteModel;

    /**
     * 发送消息是否校验关系链 TODO 需要持久化到数据库表
     */
    private boolean sendMessageCheckFriend;

    /**
     * 发送消息是否校验黑名单 TODO 需要持久化到数据库表
     */
    private boolean sendMessageCheckBlack;

    /**
     * 一致性哈希所使用的底层数据结构
     */
    private Integer consistentHashModel;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 用户资料变更之后回调开关
     */
    private boolean modifyUserAfterCallback;

    /**
     * 添加好友之后回调开关
     */
    private boolean addFriendAfterCallback;

    /**
     * 添加好友之前回调开关
     */
    private boolean addFriendBeforeCallback;

    /**
     * 修改好友之后回调开关
     */
    private boolean modifyFriendAfterCallback;

    /**
     * 删除好友之后回调开关
     */
    private boolean deleteFriendAfterCallback;

    /**
     * 添加黑名单之后回调开关
     */
    private boolean addFriendShipBlackAfterCallback;

    /**
     * 删除黑名单之后回调开关
     */
    private boolean deleteFriendShipBlackAfterCallback;

    /**
     * 创建群聊之后回调开关
     */
    private boolean createGroupAfterCallback;

    /**
     * 修改群聊之后回调开关
     */
    private boolean modifyGroupAfterCallback;

    /**
     * 解散群聊之后回调开关
     */
    private boolean destroyGroupAfterCallback;

    /**
     * 删除群成员之后回调
     */
    private boolean deleteGroupMemberAfterCallback;

    /**
     * 拉人入群之前回调
     */
    private boolean addGroupMemberBeforeCallback;

    /**
     * 拉人入群之后回调
     */
    private boolean addGroupMemberAfterCallback;

}
