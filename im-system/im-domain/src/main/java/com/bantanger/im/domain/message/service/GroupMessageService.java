package com.bantanger.im.domain.message.service;

import com.alibaba.fastjson.JSON;
import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.error.MessageErrorCode;
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
import com.bantanger.im.service.utils.ThreadPoolUtil;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 群聊逻辑
 *
 * @author BanTanger 半糖
 * @Date 2023/4/4 23:04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private static final String MODULE_NAME = "GROUP";
    private final RedisSequence redisSequence;
    private final MessageProducer messageProducer;
    private final CheckSendMessage checkSendMessageImpl;
    private final ImGroupMemberService imGroupMemberServiceImpl;
    private final MessageStoreService messageStoreServiceImpl;

    /**
     * 线程池优化群聊消息处理逻辑
     */
    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR;

    static {
        THREAD_POOL_EXECUTOR = ThreadPoolUtil.getIoTargetThreadPool(MODULE_NAME);
    }

    public void processor(GroupChatMessageContent messageContent) {
        // 日志打印
        log.info("[{}] 消息 ID [{}] 开始处理", MODULE_NAME, messageContent.getMessageId());

        // 设置临时缓存，避免消息无限制重发，当缓存失效，直接重新构建新消息进行处理
        String messageCacheByMessageId = messageStoreServiceImpl
                .getMessageCacheByMessageId(messageContent.getAppId(), messageContent.getMessageId());
        if (messageCacheByMessageId != null &&
                messageCacheByMessageId.equals(MessageErrorCode.MESSAGE_CACHE_EXPIRE.getError())) {
            // 说明缓存过期，服务端向客户端发送 ack 要求客户端重新生成 messageId
            // 不做处理。直到客户端计时器超时
            return;
        }
        GroupChatMessageContent messageCache =
                JSON.parseObject(messageCacheByMessageId, GroupChatMessageContent.class);
        if (messageCache != null) {
            THREAD_POOL_EXECUTOR.execute(() -> {
                // 线程池执行消息同步，发送，回应等任务流程
                doThreadPoolTask(messageCache);
            });
            return;
        }
        // 定义群聊消息的 Sequence, 客户端根据 seq 进行排序
        // key: appId + Seq + (from + toId) / groupId
        long seq = redisSequence.doGetSeq(messageContent.getAppId()
                + Constants.SeqConstants.GroupMessageSeq
                + messageContent.getGroupId());
        messageContent.setMessageSequence(seq);

        // 1. 消息持久化落库（不等待执行结果）
        CompletableFuture.runAsync(() -> {
            messageStoreServiceImpl.storeGroupMessage(messageContent);
        }, THREAD_POOL_EXECUTOR);

        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> {
            // 2. 查询群组所有成员进行消息分发
            List<String> groupMemberIds = imGroupMemberServiceImpl
                    .getGroupMemberId(messageContent.getGroupId(), messageContent.getAppId());
            messageContent.setMemberIds(groupMemberIds);

            return messageContent;
        }, THREAD_POOL_EXECUTOR).thenApplyAsync(messageContentWithMembers -> {
            // 3. 在异步持久化之后执行离线消息存储
            OfflineMessageContent offlineMessage = getOfflineMessage(messageContentWithMembers);
            offlineMessage.setToId(messageContentWithMembers.getGroupId());
            messageStoreServiceImpl.storeGroupOfflineMessage(
                    offlineMessage, messageContentWithMembers.getMemberIds());

            return messageContentWithMembers;
        }, THREAD_POOL_EXECUTOR).thenAcceptAsync(finalMessageContent -> {
            // 4. 线程池执行消息同步，发送，回应等任务流程
            doThreadPoolTask(finalMessageContent);
            // 5. 消息缓存
            messageStoreServiceImpl.setMessageCacheByMessageId(finalMessageContent.getAppId(), finalMessageContent.getMessageId(), finalMessageContent);

            log.info("[{}] 消息 ID [{}] 处理完成",
                    MODULE_NAME, finalMessageContent.getMessageId());
        }, THREAD_POOL_EXECUTOR);

        // 等待所有任务完成
        future.join();
    }

    private void doThreadPoolTask(GroupChatMessageContent messageContent) {
        // 2. 返回应答报文 ACK 给自己
        ack(messageContent, ResponseVO.successResponse());
        // 3. 发送消息，同步发送方多端设备
        syncToSender(messageContent);
        // 4. 发送消息给对方所有在线端
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

        sendMessageResp.setMessageId(message.getMessageId());
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
     *
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
     *
     * @param messageContent
     * @param responseVO
     */
    protected void ack(MessageContent messageContent, ResponseVO responseVO) {
        log.info("[{}] msg ack, msgId = {}, checkResult = {}",
                MODULE_NAME, messageContent.getMessageId(), responseVO.getCode());

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
     *
     * @param messageContent
     */
    protected void syncToSender(MessageContent messageContent) {
        log.info("[{}] 发送方消息同步", MODULE_NAME);
        messageProducer.sendToUserExceptClient(messageContent.getFromId(),
                GroupEventCommand.MSG_GROUP, messageContent, messageContent);
    }

    /**
     * [群聊] 消息发送【接收端所有[在线]端都需要接收消息】
     *
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
