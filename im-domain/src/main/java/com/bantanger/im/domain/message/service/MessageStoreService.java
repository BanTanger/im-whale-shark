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
import org.apache.commons.lang3.StringUtils;
import org.omg.CORBA.TIMEOUT;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 消息(MQ 异步)落库持久化
 *
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:50
 */
@Service
public class MessageStoreService {

    @Resource
    RabbitTemplate rabbitTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

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
     * 群聊消息持久化(MQ 异步持久化)
     * @param messageContent
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
     * 通过 MessageId 设置消息缓存
     * @param appId
     * @param messageId
     * @param messageContent
     */
    public void setMessageCacheByMessageId(Integer appId, String messageId, Object messageContent) {
        String key = appId + Constants.RedisConstants.CacheMessage + messageId;
        // 过期时间设置成 5 分钟
        stringRedisTemplate.opsForValue().set(key, JSONObject.toJSONString(messageContent), 300, TimeUnit.SECONDS);
    }

    /**
     * 通过 MessageId 获取消息缓存
     * @param appId
     * @param messageId
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getMessageCacheByMessageId(Integer appId, String messageId, Class<T> clazz) {
        String key = appId + Constants.RedisConstants.CacheMessage + messageId;
        String msgCache = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isBlank(msgCache)) {
            return null;
        }
        return JSONObject.parseObject(msgCache, clazz);
    }

    /**
     * messageContent 转换成 MessageBody
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
