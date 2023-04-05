package com.bantanger.im.domain.message.service;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.friend.DelFlagEnum;
import com.bantanger.im.common.model.message.store.DoStoreGroupMessageDto;
import com.bantanger.im.common.model.message.store.DoStoreP2PMessageDto;
import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.common.model.message.MessageBody;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.domain.message.dao.ImGroupMessageHistoryEntity;
import com.bantanger.im.domain.message.dao.ImMessageBodyEntity;
import com.bantanger.im.domain.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.bantanger.im.domain.message.dao.mapper.ImMessageBodyMapper;
import com.bantanger.im.service.support.ids.SnowflakeIdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 消息(MQ 异步)落库持久化
 *
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:50
 */
@Service
public class MessageStoreService {

    @Resource
    ImMessageBodyMapper messageBodyMapper;

    @Resource
    ImGroupMessageHistoryMapper groupMessageHistoryMapper;

    @Resource
    RabbitTemplate rabbitTemplate;

    /**
     * 单聊消息持久化(MQ 异步持久化)
     * @param messageContent
     */
    public void storeP2PMessage(MessageContent messageContent) {
        // 将 MessageContent 转换成 MessageBody
        MessageBody messageBody = extractMessageBody(messageContent);
        DoStoreP2PMessageDto dto = new DoStoreP2PMessageDto();
        messageContent.setMessageKey(messageBody.getMessageKey());
        dto.setMessageContent(messageContent);
        dto.setMessageBody(messageBody);
        // MQ 异步持久化, 将实体消息传递给 MQ
        rabbitTemplate.convertAndSend(
                Constants.RabbitmqConstants.StoreP2PMessage, "",
                JSONObject.toJSONString(dto));
    }

    /**
     * 群聊消息持久化
     * @param messageContent
     * @return
     */
    public void storeGroupMessage(GroupChatMessageContent messageContent) {
        MessageBody messageBody = extractMessageBody(messageContent);
        DoStoreGroupMessageDto doStoreGroupMessageDto = new DoStoreGroupMessageDto();
        doStoreGroupMessageDto.setMessageBody(messageBody);
        doStoreGroupMessageDto.setGroupChatMessageContent(messageContent);
        rabbitTemplate.convertAndSend(
                Constants.RabbitmqConstants.StoreGroupMessage, "",
                JSONObject.toJSONString(doStoreGroupMessageDto));
        messageContent.setMessageKey(messageBody.getMessageKey());
    }

    /**
     * 【读扩散】群聊历史记录存储实体类
     *
     * @param messageContent
     * @param imMessageBodyEntity
     * @return
     */
    private ImGroupMessageHistoryEntity extractToGroupMessageHistory(
            GroupChatMessageContent messageContent, ImMessageBodyEntity imMessageBodyEntity
    ) {
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity = new ImGroupMessageHistoryEntity();
        imGroupMessageHistoryEntity.setAppId(messageContent.getAppId());
        imGroupMessageHistoryEntity.setFromId(messageContent.getFromId());
        imGroupMessageHistoryEntity.setGroupId(messageContent.getGroupId());
        imGroupMessageHistoryEntity.setMessageTime(messageContent.getMessageTime());

        imGroupMessageHistoryEntity.setMessageKey(imMessageBodyEntity.getMessageKey());
        imGroupMessageHistoryEntity.setMessageTime(imMessageBodyEntity.getMessageTime());
        imGroupMessageHistoryEntity.setCreateTime(System.currentTimeMillis());

        return imGroupMessageHistoryEntity;
    }

    public MessageBody extractMessageBody(MessageContent messageContent) {
        MessageBody messageBody = new MessageBody();
        messageBody.setAppId(messageContent.getAppId());
        // TODO 消息唯一 ID 通过雪花算法生成
        messageBody.setMessageKey(SnowflakeIdWorker.nextId());
        messageBody.setCreateTime(System.currentTimeMillis());
        // TODO 设置消息加密密钥
        messageBody.setSecurityKey("");
        messageBody.setExtra(messageContent.getExtra());
        messageBody.setDelFlag(DelFlagEnum.NORMAL.getCode());
        messageBody.setMessageTime(messageContent.getMessageTime());
        messageBody.setMessageBody(messageContent.getMessageBody());
        return messageBody;
    }

    private ImMessageBodyEntity getMsgBody(MessageBody messageBody) {
        ImMessageBodyEntity imMessageBodyEntity = new ImMessageBodyEntity();
        imMessageBodyEntity.setAppId(messageBody.getAppId());
        imMessageBodyEntity.setMessageKey(messageBody.getMessageKey());
        imMessageBodyEntity.setMessageBody(messageBody.getMessageBody());
        imMessageBodyEntity.setSecurityKey(messageBody.getSecurityKey());
        imMessageBodyEntity.setMessageTime(messageBody.getMessageTime());
        imMessageBodyEntity.setCreateTime(messageBody.getCreateTime());
        imMessageBodyEntity.setExtra(messageBody.getExtra());
        imMessageBodyEntity.setDelFlag(messageBody.getDelFlag());
        return imMessageBodyEntity;
    }
}
