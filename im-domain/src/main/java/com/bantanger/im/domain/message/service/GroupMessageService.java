package com.bantanger.im.domain.message.service;

import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.message.content.GroupChatMessageContent;
import com.bantanger.im.common.model.message.content.MessageContent;
import com.bantanger.im.common.model.message.content.OfflineMessageContent;
import com.bantanger.im.domain.group.model.req.SendGroupMessageReq;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import com.bantanger.im.domain.message.model.resp.SendMessageResp;
import com.bantanger.im.domain.message.seq.RedisSequence;
import com.bantanger.im.domain.message.service.check.CheckSendMessage;
import com.bantanger.im.domain.message.service.store.MessageStoreService;
import com.bantanger.im.service.sendmsg.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.RedisClientInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    CheckSendMessage checkSendMessageImpl;

    @Resource
    MessageProducer messageProducer;

    @Resource
    ImGroupMemberService imGroupMemberServiceImpl;

    @Resource
    MessageStoreService messageStoreServiceImpl;

    @Resource
    RedisSequence redisSequence;

    /** 线程池优化单聊消息处理逻辑 */
    private final ThreadPoolExecutor threadPoolExecutor;

    {
        final AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                // 任务队列存储超过核心线程数的任务
                new LinkedBlockingDeque<>(1000), r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("[GROUP] message-process-thread-" + num.getAndIncrement());
            return thread;
        });
    }

    public void processor(GroupChatMessageContent messageContent) {
        // 日志打印
        log.info("消息 ID [{}] 开始处理", messageContent.getMessageId());

        GroupChatMessageContent messageCache = messageStoreServiceImpl.getMessageCacheByMessageId(messageContent.getAppId(), messageContent.getMessageId(), GroupChatMessageContent.class);
        if (messageCache != null) {
            threadPoolExecutor.execute(() -> {
                // 线程池执行消息同步，发送，回应等任务流程
                doThreadPoolTask(messageContent);
            });
        }
        // 定义群聊消息的 Sequence, 客户端根据 seq 进行排序
        // key: appId + Seq + (from + toId) / groupId
        long seq = redisSequence.doGetSeq(messageContent.getAppId()
                + Constants.SeqConstants.GroupMessageSeq
                + messageContent.getGroupId());
        messageContent.setMessageSequence(seq);

        threadPoolExecutor.execute(() -> {
            // 1. 消息持久化落库
            messageStoreServiceImpl.storeGroupMessage(messageContent);

            // 查询群组所有成员进行消息分发
            List<String> groupMemberIds = imGroupMemberServiceImpl
                    .getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId());

            messageContent.setMemberIds(groupMemberIds);

            // 2.在异步持久化之后执行离线消息存储
            OfflineMessageContent offlineMessage = getOfflineMessage(messageContent);
            offlineMessage.setToId(messageContent.getGroupId());
            messageStoreServiceImpl.storeGroupOfflineMessage(offlineMessage, groupMemberIds);

            // 线程池执行消息同步，发送，回应等任务流程
            doThreadPoolTask(messageContent);

            // 消息缓存
            messageStoreServiceImpl.setMessageCacheByMessageId(
                    messageContent.getAppId(), messageContent.getMessageId(), messageContent);
        });

        log.info("消息 ID [{}] 处理完成", messageContent.getMessageId());
    }

    private void doThreadPoolTask(GroupChatMessageContent messageContent) {
        // 2. 返回应答报文 ACK 给自己
        ack(messageContent, ResponseVO.successResponse());
        // 3. 发送消息，同步发送方多端设备
        syncToSender(messageContent);
        // 4. 发送消息给对方所有在线端(TODO 离线端也要做消息同步)
        dispatchMessage(messageContent);
    }

    private OfflineMessageContent getOfflineMessage(GroupChatMessageContent messageContent) {
        OfflineMessageContent offlineMessageContent = new OfflineMessageContent();
        offlineMessageContent.setAppId(messageContent.getAppId());
        offlineMessageContent.setMessageKey(messageContent.getMessageKey());
        offlineMessageContent.setMessageBody(messageContent.getMessageBody());
        offlineMessageContent.setMessageTime(messageContent.getMessageTime());
        offlineMessageContent.setExtra(messageContent.getExtra());
        offlineMessageContent.setFromId(messageContent.getFromId());
        offlineMessageContent.setToId(messageContent.getToId());
        offlineMessageContent.setMessageSequence(messageContent.getMessageSequence());
        return offlineMessageContent;
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

        messageStoreServiceImpl.storeGroupMessage(message);

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
    public ResponseVO serverPermissionCheck(String fromId, String groupId, Integer appId) {
        return checkSendMessageImpl.checkGroupMessage(fromId, groupId, appId);
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
        messageContent.getMemberIds().stream()
                // 排除自己
                .filter(memberId -> !memberId.equals(messageContent.getFromId()))
                .forEach(memberId -> messageProducer.sendToUserAllClient(
                        memberId, GroupEventCommand.MSG_GROUP,
                        messageContent, messageContent.getAppId()));
    }

}
