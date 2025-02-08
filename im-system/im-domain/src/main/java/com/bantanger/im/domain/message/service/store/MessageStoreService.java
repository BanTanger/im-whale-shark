package com.bantanger.im.domain.message.service.store;

import com.bantanger.im.common.model.message.content.GroupChatMessageContent;
import com.bantanger.im.common.model.message.content.MessageContent;
import com.bantanger.im.common.model.message.content.OfflineMessageContent;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 11:22
 */
public interface MessageStoreService {

    /**
     * 单聊消息持久化(MQ 异步持久化)
     * @param messageContent
     */
    void storeP2PMessage(MessageContent messageContent);

    /**
     * 群聊消息持久化(MQ 异步持久化)
     * @param messageContent
     */
    void storeGroupMessage(GroupChatMessageContent messageContent);

    /**
     * 通过 MessageId 设置消息缓存
     * @param appId
     * @param messageId
     * @param messageContent
     */
    void setMessageCacheByMessageId(Integer appId, String messageId, Object messageContent);

    /**
     * 通过 MessageId 获取消息缓存
     * @param appId
     * @param messageId
     * @return
     */
    String getMessageCacheByMessageId(Integer appId, String messageId);

    /**
     * 【读扩散】存储单聊离线消息
     * @param messageContent
     */
    void storeOfflineMessage(OfflineMessageContent messageContent);

    /**
     * 【读扩散】存储群聊离线消息
     * @param messageContent
     * @param memberIds
     */
    void storeGroupOfflineMessage(OfflineMessageContent messageContent, List<String> memberIds);

}
