package com.bantanger.im.domain.message.service;

import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.domain.group.model.req.SendGroupMessageReq;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import com.bantanger.im.domain.message.model.resp.SendMessageResp;
import com.bantanger.im.service.sendmsg.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 群聊逻辑
 *
 * @author BanTanger 半糖
 * @Date 2023/4/4 23:04
 */
@Slf4j
@Service
public class GroupMessageService {

    @Resource
    CheckSendMessageService checkSendMessageService;

    @Resource
    MessageProducer messageProducer;

    @Resource
    ImGroupMemberService groupMemberService;

    @Resource
    MessageStoreService messageStoreService;

    public void processor(GroupChatMessageContent messageContent) {
        // 日志打印
        log.info("消息 ID [{}] 开始处理", messageContent.getMessageId());

        String fromId = messageContent.getFromId();
        String groupId = messageContent.getGroupId();
        Integer appId = messageContent.getAppId();

        // 1. 前置校验，判断发送方是否被禁言、封禁，好友关系链是否正确
        ResponseVO responseVO = serverPermissionCheck(fromId, groupId, appId);
        if (!responseVO.isOk()) {
            // ack 应答，告知 SDK 消息发送失败，状态出现异常
            ack(messageContent,responseVO);
        }

        List<String> groupMemberId = groupMemberService.getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId());
        messageContent.setMemberId(groupMemberId);
        messageContent.setGroupId(groupId);

        // 消息持久化落库
        ResponseVO msgResp = messageStoreService.storeGroupMessage(messageContent);
        if (!msgResp.isOk()) {
            log.error("消息持久化失败 {}", msgResp.getMsg());
        }

        // 2. 返回应答报文 ACK 给自己
        ack(messageContent, ResponseVO.successResponse());
        // 3. 发送消息，同步发送方多端设备
        syncToSender(messageContent);
        // 4. 发送消息给对方所有在线端(TODO 离线端也要做消息同步)
        dispatchMessage(messageContent);
        log.info("消息 ID [{}] 处理完成", messageContent.getMessageId());
    }

    public SendMessageResp send(SendGroupMessageReq req) {
        SendMessageResp sendMessageResp = new SendMessageResp();
        GroupChatMessageContent message = new GroupChatMessageContent();
        message.setAppId(req.getAppId());
        message.setClientType(req.getClientType());
        message.setImei(req.getImei());
        message.setMessageId(req.getMessageId());
        message.setFromId(req.getFromId());
        message.setMessageBody(req.getMessageBody());
        message.setMessageTime(req.getMessageTime());
        message.setGroupId(req.getGroupId());

        messageStoreService.storeGroupMessage(message);

        sendMessageResp.setMessageKey(message.getMessageKey());
        sendMessageResp.setMessageTime(System.currentTimeMillis());
        //2.发消息给同步在线端
        syncToSender(message);
        //3.发消息给对方在线端
        dispatchMessage(message);

        return sendMessageResp;

    }

    /**
     * 前置校验
     * 1. 这个用户是否被禁言 是否被禁用
     * 2. 发送方是否在群组内
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    protected ResponseVO serverPermissionCheck(String fromId, String groupId, Integer appId) {
        return checkSendMessageService.checkGroupMessage(fromId, groupId, appId);
    }

    /**
     * ACK 应答报文包装和发送
     * @param messageContent
     * @param responseVO
     */
    protected void ack(MessageContent messageContent, ResponseVO responseVO) {
        log.info("[GROUP] msg ack, msgId = {}, checkResult = {}", messageContent.getMessageId(), responseVO.getCode());

        // ack 包塞入消息 id，告知客户端端 该条消息已被成功接收
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发送消息，回传给发送方端
        messageProducer.sendToUserOneClient(messageContent.getFromId(),
                GroupEventCommand.GROUP_MSG_ACK, responseVO, messageContent
        );
    }

    /**
     * 消息同步【发送方除本端所有端消息同步】
     * @param messageContent
     */
    protected void syncToSender(MessageContent messageContent) {
        log.info("[GROUP] 发送方消息同步");
        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                GroupEventCommand.MSG_GROUP, messageContent, messageContent);
    }

    /**
     * [群聊] 消息发送【接收端所有端都需要接收消息】
     * @param messageContent
     * @return
     */
    protected void dispatchMessage(GroupChatMessageContent messageContent) {
        messageContent.getMemberId().stream()
                // 排除自己
                .filter(memberId -> !memberId.equals(messageContent.getFromId()))
                .forEach(memberId -> messageProducer.sendToUserAllClient(
                        memberId, GroupEventCommand.MSG_GROUP,
                        messageContent, messageContent.getAppId()));
    }

}