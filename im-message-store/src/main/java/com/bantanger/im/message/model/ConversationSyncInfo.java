package com.bantanger.im.message.model;

import lombok.Data;

/**
 * 会话同步信息
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class ConversationSyncInfo {

    /**
     * 会话ID（单聊为toId，群聊为groupId）
     */
    private String conversationId;

    /**
     * 会话类型（1-单聊，2-群聊）
     */
    private Integer conversationType;

    /**
     * 客户端本地最大序列号
     */
    private Long sequence;
}