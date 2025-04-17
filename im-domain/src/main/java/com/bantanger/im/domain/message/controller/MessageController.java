package com.bantanger.im.domain.message.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.message.CheckSendMessageReq;
import com.bantanger.im.domain.message.model.req.SendMessageReq;
import com.bantanger.im.domain.message.service.GroupMessageService;
import com.bantanger.im.domain.message.service.P2PMessageService;
import com.bantanger.im.domain.message.service.sync.MessageSyncService;
import com.bantanger.im.domain.message.service.sync.MessageSyncServiceImpl;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 后台发送消息控制层
 *
 * @author BanTanger 半糖
 * @Date 2023/4/5 17:06
 */
@RestController
@RequestMapping("v1/message")
public class MessageController {

    @Resource
    P2PMessageService p2PMessageService;

    @Resource
    GroupMessageService groupMessageService;

    @Resource
    MessageSyncService messageSyncServiceImpl;

    /**
     * 后台消息发送接口
     *
     * @param req
     * @return
     */
    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req) {
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

    /**
     * Feign RPC 调用 [P2P] 内部接口
     * @param req
     * @return
     */
    @RequestMapping("/p2pCheckSend")
    public ResponseVO checkP2PSend(@RequestBody @Validated CheckSendMessageReq req) {
        return p2PMessageService.serverPermissionCheck(
                req.getFromId(), req.getToId(), req.getAppId());
    }

    /**
     * Feign RPC 调用 [GROUP] 内部接口
     * @param req
     * @return
     */
    @RequestMapping("/groupCheckSend")
    public ResponseVO checkGroupSend(@RequestBody @Validated CheckSendMessageReq req) {
        return groupMessageService.serverPermissionCheck(
                req.getFromId(), req.getToId(), req.getAppId());
    }

    @RequestMapping("/syncOfflineMessageList")
    public ResponseVO syncP2POfflineMessageList(@RequestBody @Validated SyncReq req) {
        return messageSyncServiceImpl.syncOfflineMessage(req);
    }

}
