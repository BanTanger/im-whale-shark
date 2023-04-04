package com.bantanger.im.domain.message.service;

import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.message.model.MessageContent;
import com.bantanger.im.service.sendmsg.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 11:19
 */
@Slf4j
@Service
public class P2PMessageService {

    @Resource
    CheckSendMessageService checkSendMessageService;

    @Resource
    MessageProducer messageProducer;


    public void processor(MessageContent messageContent) {
        log.info("消息 ID [{}] 开始处理", messageContent.getMessageId());

        String fromId = messageContent.getFromId();
        String toId = messageContent.getToId();
        Integer appId = messageContent.getAppId();

        //前置校验
        //这个用户是否被禁言 是否被禁用
        //发送方和接收方是否是好友
        ResponseVO responseVO = imServerPermissionCheck(fromId, toId, appId);
        if (!responseVO.isOk()) {
            // ack 应答，告知 SDK 消息发送失败，状态出现异常
            ack(messageContent,responseVO);
        }

        // 1. 返回应答报文 ACK 给自己
        ack(messageContent, ResponseVO.successResponse());
        // 2. 发送消息，同步发送方多端设备
        syncToSender(messageContent);
        // 3. 发送消息给对方所有在线端(TODO 离线端也要做消息同步)
        List<ClientInfo> clientInfos = dispatchMessage(messageContent);
        log.info("消息 ID [{}] 处理完成", messageContent.getMessageId());
    }

    public ResponseVO imServerPermissionCheck(String fromId, String toId, Integer appId) {
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isOk()) {
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }

    /**
     * ACK 应答报文包装和发送
     * @param messageContent
     * @param responseVO
     */
    private void ack(MessageContent messageContent, ResponseVO responseVO) {
        log.info("msg ack, msgId = {},checkResult = {}", messageContent.getMessageId(), responseVO.getCode());

        // ack 包塞入消息 id，告知客户端端 该条消息已被成功接收
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发送消息，回传给发送方端
        messageProducer.sendToUserOneClient(messageContent.getFromId(),
                MessageCommand.MSG_ACK, responseVO, messageContent);
    }

    /**
     * 消息同步
     * @param messageContent
     */
    private void syncToSender(MessageContent messageContent) {
        messageProducer.sendToUserExceptClient(
                messageContent.getFromId(),
                MessageCommand.MSG_P2P,
                messageContent, messageContent
        );
    }

    /**
     * 消息发送
     * @param messageContent
     * @return
     */
    private List<ClientInfo> dispatchMessage(MessageContent messageContent) {
        List<ClientInfo> clientInfos = messageProducer.sendToUserAllClient(
                messageContent.getToId(),
                MessageCommand.MSG_P2P,
                messageContent,
                messageContent.getAppId()
        );
        return clientInfos;
    }
}
