package com.bantanger.im.message.model;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import java.util.List;

/**
 * 批量同步消息请求
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class BatchSyncMessageReq extends RequestBase {

    /**
     * 用户ID，消息拥有者
     */
    private String userId;

    /**
     * 会话同步信息列表
     */
    private List<ConversationSyncInfo> conversations;

    /**
     * 单个会话最大拉取消息数量
     */
    private Integer maxCount;
}