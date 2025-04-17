package com.bantanger.im.message.model;

import lombok.Data;

import java.util.List;

/**
 * 批量同步消息响应
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class BatchSyncMessageResp {

    /**
     * 会话消息响应列表
     */
    private List<Object> conversationList;
}