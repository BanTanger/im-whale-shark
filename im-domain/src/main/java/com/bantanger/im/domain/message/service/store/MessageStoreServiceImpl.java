package com.bantanger.im.domain.message.service.store;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.conversation.ConversationTypeEnum;
import com.bantanger.im.common.enums.error.MessageErrorCode;
import com.bantanger.im.common.enums.friend.DelFlagEnum;
import com.bantanger.im.common.model.message.content.OfflineMessageContent;
import com.bantanger.im.common.model.message.store.DoStoreGroupMessageDto;
import com.bantanger.im.common.model.message.store.DoStoreP2PMessageDto;
import com.bantanger.im.common.model.message.content.GroupChatMessageContent;
import com.bantanger.im.common.model.message.content.MessageBody;
import com.bantanger.im.common.model.message.content.MessageContent;
import com.bantanger.im.domain.conversation.service.ConversationService;
import com.bantanger.im.domain.conversation.service.ConversationServiceImpl;
import com.bantanger.im.service.config.AppConfig;
import com.bantanger.im.service.support.ids.SnowflakeIdWorker;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 消息(MQ 异步)落库持久化
 *
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:50
 */
@Service
@RequiredArgsConstructor
public class MessageStoreServiceImpl implements MessageStoreService {

    private final RabbitTemplate rabbitTemplate;
    private final AppConfig appConfig;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
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

    @Override
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

    @Override
    public void setMessageCacheByMessageId(Integer appId, String messageId, Object messageContent) {
        String key = appId + Constants.RedisConstants.CacheMessage + messageId;
        // 过期时间设置成 5 分钟
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(messageContent), 300, TimeUnit.SECONDS);
    }

    @Override
    public String getMessageCacheByMessageId(Integer appId, String messageId) {
        String key = appId + Constants.RedisConstants.CacheMessage + messageId;
        // 先判断是否有这个键值，由于 redis 两种删除策略：惰性删除、定期删除
        // 惰性删除，键值过期依然会有 key，当有线程获取 value 才会删除 key
        // 两种情况：redis 获取不到 value
        // 1. 首次进入，没有设置缓存，not set， getMessageCacheByMessageId == null
        // 2. 重复进入，但是缓存过期，value = null
        Boolean hasKey = stringRedisTemplate.hasKey(key);
        if (hasKey == null || !hasKey) {
            // 没有 key，说明根本没有缓存，或者是定期删除恰好删除了，直接返回 null
            return null;
        }
        Long expireTime = stringRedisTemplate.getExpire(key);
        // 键值已过期
        if (expireTime <= 0) {
            stringRedisTemplate.delete(key);
            return MessageErrorCode.MESSAGE_CACHE_EXPIRE.getError();
        }

        String msgCache = stringRedisTemplate.opsForValue().get(key);
        return msgCache;
    }

    @Override
    public void storeOfflineMessage(OfflineMessageContent offlineMessage) {
        // 获取 fromId 离线消息队列
        getOfflineMsgQueue(offlineMessage, offlineMessage.getFromId(), offlineMessage.getToId(), ConversationTypeEnum.P2P);
        // 获取 toId 离线消息队列
        getOfflineMsgQueue(offlineMessage, offlineMessage.getToId(), offlineMessage.getFromId(), ConversationTypeEnum.P2P);
    }

    @Override
    public void storeGroupOfflineMessage(OfflineMessageContent offlineMessage, List<String> memberIds) {
        // 对群成员执行 getOfflineMsgQueue 逻辑
        memberIds.forEach(memberId -> getOfflineMsgQueue(
                offlineMessage, memberId,
                offlineMessage.getToId(),
                ConversationTypeEnum.GROUP
        ));
    }

    /**
     * 获取 fromId 的离线消息队列
     * @param offlineMessage
     * @param fromId
     * @param toId
     * @param conversationType
     */
    private void getOfflineMsgQueue(OfflineMessageContent offlineMessage, String fromId, String toId, ConversationTypeEnum conversationType) {
        // 获取用户离线消息队列
        String userKey = offlineMessage.getAppId() + Constants.RedisConstants.OfflineMessage + fromId;

        ZSetOperations<String, String> operations = stringRedisTemplate.opsForZSet();
        if (operations.zCard(userKey) > appConfig.getOfflineMessageCount()) {
            // 如果队列数据超过阈值，删除最前面的数据
            operations.removeRange(userKey, 0, 0);
        }

        offlineMessage.setConversationType(conversationType.getCode());
        offlineMessage.setConversationId(ConversationService.convertConversationId(
                conversationType.getCode(), fromId, toId
        ));
        // 插入数据，messageKey 作为分值
        operations.add(userKey, JSONObject.toJSONString(offlineMessage), offlineMessage.getMessageKey());
    }

    /**
     * messageContent 转换成 MessageBody
     *
     * @param messageContent
     * @return
     */
    private MessageBody extractMessageBody(MessageContent messageContent) {
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

}
