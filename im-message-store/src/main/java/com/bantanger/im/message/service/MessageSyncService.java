package com.bantanger.im.message.service;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.message.model.BatchSyncMessageReq;
import com.bantanger.im.message.model.SyncGroupMessageReq;
import com.bantanger.im.message.model.SyncP2PMessageReq;

/**
 * 消息同步服务接口
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
public interface MessageSyncService {

    /**
     * 同步单聊消息
     * 
     * @param req 同步请求
     * @return 同步结果
     */
    ResponseVO syncP2PMessage(SyncP2PMessageReq req);

    /**
     * 同步群聊消息
     * 
     * @param req 同步请求
     * @return 同步结果
     */
    ResponseVO syncGroupMessage(SyncGroupMessageReq req);
    
    /**
     * 批量同步多个会话的消息
     * 
     * @param req 批量同步请求
     * @return 批量同步结果
     */
    ResponseVO batchSyncMessage(BatchSyncMessageReq req);
}