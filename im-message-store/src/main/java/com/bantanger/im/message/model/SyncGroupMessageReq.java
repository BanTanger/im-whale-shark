package com.bantanger.im.message.model;

import com.bantanger.im.common.model.SyncReq;
import lombok.Data;

/**
 * 群聊消息同步请求类
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class SyncGroupMessageReq extends SyncReq {

    /**
     * 用户ID，消息拥有者
     */
    private String userId;

    /**
     * 群组ID
     */
    private String groupId;

}