package com.bantanger.im.domain.message.service;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.error.MessageErrorCode;
import com.bantanger.im.common.enums.friend.DelFlagEnum;
import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.common.model.message.MessageBody;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.domain.message.dao.ImGroupMessageHistoryEntity;
import com.bantanger.im.domain.message.dao.ImMessageBodyEntity;
import com.bantanger.im.domain.message.dao.ImMessageHistoryEntity;
import com.bantanger.im.domain.message.dao.mapper.ImGroupMessageHistoryMapper;
import com.bantanger.im.domain.message.dao.mapper.ImMessageBodyMapper;
import com.bantanger.im.domain.message.dao.mapper.ImMessageHistoryMapper;
import com.bantanger.im.service.support.ids.SnowflakeIdWorker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 消息落库持久化
 *
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:50
 */
@Service
public class MessageStoreService {

    @Resource
    ImMessageBodyMapper messageBodyMapper;

    @Resource
    ImMessageHistoryMapper messageHistoryMapper;

    @Resource
    ImGroupMessageHistoryMapper groupMessageHistoryMapper;

    /**
     * 单聊消息持久化
     * @param messageContent
     * @return
     */
    @Transactional
    public ResponseVO storeP2PMessage(MessageContent messageContent) {
        // 1. 将 MessageContent 转换成 MessageBody
        MessageBody messageBody = extractMessageBody(messageContent);
        // 2. 将 MessageBody 插入数据库表 im_message_body 中持久化
        ImMessageBodyEntity imMessageBodyEntity = getMsgBody(messageBody);
        int insert = messageBodyMapper.insert(imMessageBodyEntity);
        if (insert != 1) {
            return ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_PERSISTENCE_ERROR);
        }
        // 3. 将 MessageContent, imMessageBodyEntity 批量插入数据库表 im_message_history 中持久化
        List<ImMessageHistoryEntity> imMessageHistoryEntities =
                extractToP2PMessageHistory(messageContent, imMessageBodyEntity);
        Integer integer = messageHistoryMapper.insertBatchSomeColumn(imMessageHistoryEntities);
        if (integer == null || integer != 1) {
            return ResponseVO.errorResponse(MessageErrorCode.MESSAGEHISTORY_PERSISTENCE_ERROR);
        }
        messageBody.setMessageKey(imMessageBodyEntity.getMessageKey());
        return ResponseVO.successResponse();
    }

    /**
     * 群聊消息持久化
     * @param messageContent
     * @return
     */
    @Transactional
    public ResponseVO storeGroupMessage(GroupChatMessageContent messageContent) {
        MessageBody messageBody = extractMessageBody(messageContent);
        ImMessageBodyEntity imMessageBodyEntity = getMsgBody(messageBody);
        // 将 MessageBody 插入到数据库表 im_message_body 中
        int msgBodyInsert = messageBodyMapper.insert(imMessageBodyEntity);
        if (msgBodyInsert != 1) {
            return ResponseVO.errorResponse(MessageErrorCode.MESSAGEBODY_PERSISTENCE_ERROR);
        }
        // 插入数据库表 im_message_group_history 中
        ImGroupMessageHistoryEntity imGroupMessageHistoryEntity =
                extractToGroupMessageHistory(messageContent, imMessageBodyEntity);
        int msgGroupHistoryInsert = groupMessageHistoryMapper.insert(imGroupMessageHistoryEntity);
        if (msgGroupHistoryInsert != 1) {
            return ResponseVO.errorResponse(MessageErrorCode.MESSAGEHISTORY_PERSISTENCE_ERROR);
        }
        return ResponseVO.successResponse();
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

    /**
     * 【写扩散】双方消息冗余备份，并纪录消息拥有者 ownId
     * @param messageContent
     * @param imMessageBodyEntity
     * @return
     */
    private List<ImMessageHistoryEntity> extractToP2PMessageHistory(
            MessageContent messageContent, ImMessageBodyEntity imMessageBodyEntity
    ) {
        List<ImMessageHistoryEntity> list = new ArrayList<>();
        // 己方历史消息记录表 DAO 实体类
        ImMessageHistoryEntity fromMsgHistory = getMsgHistory(messageContent.getFromId(), messageContent, imMessageBodyEntity);
        // 对方历史消息记录表 DAO 实体类
        ImMessageHistoryEntity toMsgHistory = getMsgHistory(messageContent.getToId(), messageContent, imMessageBodyEntity);

        list.add(fromMsgHistory);
        list.add(toMsgHistory);
        return list;
    }

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

    private ImMessageHistoryEntity getMsgHistory(String userId, MessageContent msgContent, ImMessageBodyEntity msgBody) {
        ImMessageHistoryEntity msgHistory = new ImMessageHistoryEntity();
        msgHistory.setAppId(msgContent.getAppId());
        msgHistory.setFromId(msgContent.getFromId());
        msgHistory.setToId(msgContent.getToId());
        msgHistory.setMessageTime(msgContent.getMessageTime());
        // 设置消息拥有者
        msgHistory.setOwnerId(userId);
        msgHistory.setMessageKey(msgBody.getMessageKey());
        msgHistory.setCreateTime(System.currentTimeMillis());
        return msgHistory;
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
