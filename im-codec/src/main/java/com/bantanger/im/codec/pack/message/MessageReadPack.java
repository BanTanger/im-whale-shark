package com.bantanger.im.codec.pack.message;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 11:14
 */
@Data
public class MessageReadPack {

    /** 消息已读偏序 */
    private long messageSequence;

    /** 要么 fromId + toId */
    private String fromId;
    private String toId;

    /** 要么 groupId */
    private String groupId;

    /** 标识消息来源于单聊还是群聊 */
    private Integer conversationType;

}
