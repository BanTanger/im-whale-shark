package com.bantanger.im.message.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.message.model.BatchSyncMessageReq;
import com.bantanger.im.message.model.SyncGroupMessageReq;
import com.bantanger.im.message.model.SyncP2PMessageReq;
import com.bantanger.im.message.service.MessageSyncService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 消息同步控制器
 * 
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@RestController
@RequestMapping("v1/message")
public class MessageSyncController {

    @Resource
    private MessageSyncService messageSyncService;

    /**
     * 同步单聊消息
     * 
     * @param req 同步请求
     * @return 同步结果
     */
    @RequestMapping("/syncP2PMessage")
    public ResponseVO syncP2PMessage(@RequestBody @Validated SyncP2PMessageReq req) {
        return messageSyncService.syncP2PMessage(req);
    }

    /**
     * 同步群聊消息
     * 
     * @param req 同步请求
     * @return 同步结果
     */
    @RequestMapping("/syncGroupMessage")
    public ResponseVO syncGroupMessage(@RequestBody @Validated SyncGroupMessageReq req) {
        return messageSyncService.syncGroupMessage(req);
    }
    
    /**
     * 批量同步多个会话的消息
     * 
     * @param req 批量同步请求
     * @return 批量同步结果
     */
    @RequestMapping("/batchSyncMessage")
    public ResponseVO batchSyncMessage(@RequestBody @Validated BatchSyncMessageReq req) {
        return messageSyncService.batchSyncMessage(req);
    }
}