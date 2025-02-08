package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * 创建群组通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class CreateGroupPack {

    private String groupId;

    private Integer appId;

    //群主id
    private String ownerId;

    //群类型 1私有群（类似微信） 2公开群(类似qq）
    private Integer groupType;

    private String groupName;

    private Integer mute;// 是否全员禁言，0 不禁言；1 全员禁言。

    //    申请加群选项包括如下几种：
//    0 表示禁止任何人申请加入
//    1 表示需要群主或管理员审批
//    2 表示允许无需审批自由加入群组
    private Integer applyJoinType;

    private Integer privateChat; //是否禁止私聊，0 允许群成员发起私聊；1 不允许群成员发起私聊。

    private String introduction;//群简介

    private String notification;//群公告

    private String photo;//群头像

    private Integer status;//群状态 0正常 1解散

    private Long sequence;

    private Long createTime;

    private String extra;

}
