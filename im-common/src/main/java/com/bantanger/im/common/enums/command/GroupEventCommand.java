package com.bantanger.im.common.enums.command;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
public enum GroupEventCommand implements Command {

    /**
     * 推送申请入群通知 2000 --> 0x7d0
     */
    JOIN_GROUP(0x7d0),

    /**
     * 推送添加群成员 2001 --> 0x7d1，通知给所有管理员和本人
     */
    ADDED_MEMBER(0x7d1),

    /**
     * 推送创建群组通知 2002 --> 0x7d2，通知给所有人
     */
    CREATED_GROUP(0x7d2),

    /**
     * 推送更新群组通知 2003 --> 0x7d3，通知给所有人
     */
    UPDATED_GROUP(0x7d3),

    /**
     * 推送退出群组通知 2004 --> 0x7d4，通知给管理员和操作人
     */
    EXIT_GROUP(0x7d4),

    /**
     * 推送修改群成员通知 2005 --> 0x7d5，通知给管理员和被操作人
     */
    UPDATED_MEMBER(0x7d5),

    /**
     * 推送删除群成员通知 2006 --> 0x7d6，通知给所有群成员和被踢人
     */
    DELETED_MEMBER(0x7d6),

    /**
     * 推送解散群通知 2007 --> 0x7d7，通知所有人
     */
    DESTROY_GROUP(0x7d7),

    /**
     * 推送转让群主 2008 --> 0x7d8，通知所有人
     */
    TRANSFER_GROUP(0x7d8),

    /**
     * 禁言群 2009 --> 0x7d9，通知所有人
     */
    MUTE_GROUP(0x7d9),

    /**
     * 禁言/解禁 群成员 2010 --> 0x7da，通知管理员和被操作人
     */
    SPEAK_GROUP_MEMBER(0x7da),

    /**
     * 群聊消息收发 2104 --> 0x838
     */
    MSG_GROUP(0x838),

    /**
     * 发送消息已读 2106 --> 0x83a
     */
    MSG_GROUP_READIED(0x83a),

    /**
     * 消息已读通知给同步端 2053 --> 0x805
     */
    MSG_GROUP_READIED_NOTIFY(0x805),

    /**
     * 消息已读回执，给原消息发送方 2054 --> 0x806
     */
    MSG_GROUP_READIED_RECEIPT(0x806),

    /**
     * 群聊消息 ack 2047 --> 0x7ff
     */
    GROUP_MSG_ACK(0x7ff),

    ;

    private Integer command;

    GroupEventCommand(int command) {
        this.command = command;
    }


    @Override
    public Integer getCommand() {
        return command;
    }

}
