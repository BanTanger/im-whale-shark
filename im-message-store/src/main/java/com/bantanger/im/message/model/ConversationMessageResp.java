package com.bantanger.im.message.model;

import lombok.Data;

import java.util.List;

/**
 * 会话消息响应
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class ConversationMessageResp<T> {

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 会话类型（1-单聊，2-群聊）
     */
    private Integer conversationType;

    /**
     * 服务端最大序列号
     */
    private Long maxSequence;

    /**
     * 是否拉取完成
     */
    private boolean isCompleted;

    /**
     * 消息列表
     */
    private List<T> messages;
}