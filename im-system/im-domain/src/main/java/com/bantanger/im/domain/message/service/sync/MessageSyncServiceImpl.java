package com.bantanger.im.domain.message.service.sync;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.message.MessageReadPack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.SyncResp;
import com.bantanger.im.common.model.message.MessageReceiveAckContent;
import com.bantanger.im.common.model.message.content.OfflineMessageContent;
import com.bantanger.im.common.model.message.read.MessageReadContent;
import com.bantanger.im.domain.conversation.service.ConversationService;
import com.bantanger.im.service.sendmsg.MessageProducer;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 消息同步服务类
 * 用于处理消息接收确认，同步等操作
 * @author BanTanger 半糖
 * @Date 2023/4/6 23:06
 */
@Service
public class MessageSyncServiceImpl implements MessageSyncService {

    @Resource
    MessageProducer messageProducer;

    @Resource
    ConversationService conversationServiceImpl;

    @Resource
    RedisTemplate redisTemplate;

    @Override
    public void receiveMark(MessageReceiveAckContent pack) {
        // 确认接收 ACK 发送给在线目标用户全端
        messageProducer.sendToUserAllClient(pack.getToId(),
                MessageCommand.MSG_RECEIVE_ACK, pack, pack.getAppId());
    }

    @Override
    public void readMark(MessageReadContent messageContent, Command notify, Command receipt) {
        conversationServiceImpl.messageMarkRead(messageContent);
        MessageReadPack messageReadPack = Content2Pack(messageContent);
        syncToSender(messageReadPack, messageContent, notify);
        // 防止自己给自己发送消息
        if (!messageContent.getFromId().equals(messageContent.getToId())) {
            // 发送给对方
            messageProducer.sendToUserAllClient(
                    messageContent.getToId(),
                    receipt, messageReadPack,
                    messageContent.getAppId()
            );
        }
    }

    @Override
    public ResponseVO syncOfflineMessage(SyncReq req) {

        SyncResp<OfflineMessageContent> resp = new SyncResp<>();

        String key = req.getAppId() + ":" + Constants.RedisConstants.OfflineMessage + ":" + req.getOperater();
        Long maxSeq = 0L;
        ZSetOperations zSetOperations = redisTemplate.opsForZSet();
        // 获取最大的 seq
        Set set = zSetOperations.reverseRangeWithScores(key, 0, 0);
        if(!CollectionUtils.isEmpty(set)){
            List list = new ArrayList(set);
            DefaultTypedTuple o = (DefaultTypedTuple) list.get(0);
            maxSeq = Objects.requireNonNull(o.getScore()).longValue();
        }

        List<OfflineMessageContent> respList = new ArrayList<>();
        resp.setMaxSequence(maxSeq);

        Set<ZSetOperations.TypedTuple> querySet = zSetOperations.rangeByScoreWithScores(
                key, req.getLastSequence(), maxSeq, 0, req.getMaxLimit());
        for (ZSetOperations.TypedTuple<String> typedTuple : querySet) {
            String value = typedTuple.getValue();
            OfflineMessageContent offlineMessageContent = JSONObject.parseObject(value, OfflineMessageContent.class);
            respList.add(offlineMessageContent);
        }
        resp.setDataList(respList);

        if(!CollectionUtils.isEmpty(respList)){
            OfflineMessageContent offlineMessageContent = respList.get(respList.size() - 1);
            resp.setCompleted(maxSeq <= offlineMessageContent.getMessageKey());
        }

        return ResponseVO.successResponse(resp);
    }

    private void syncToSender(MessageReadPack pack, MessageReadContent content, Command command) {
        messageProducer.sendToUserExceptClient(content.getFromId(), command, pack, content);
    }
    
    private MessageReadPack Content2Pack(MessageReadContent messageContent) {
        MessageReadPack messageReadPack = new MessageReadPack();
        messageReadPack.setMessageSequence(messageContent.getMessageSequence());
        messageReadPack.setFromId(messageContent.getFromId());
        messageReadPack.setToId(messageContent.getToId());
        messageReadPack.setGroupId(messageContent.getGroupId());
        messageReadPack.setConversationType(messageContent.getConversationType());
        return messageReadPack;
    }

}
