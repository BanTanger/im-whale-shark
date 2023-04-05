package com.bantanger.im.domain.message.service;

import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.domain.message.model.req.SendMessageReq;
import com.bantanger.im.domain.message.model.resp.SendMessageResp;
import com.bantanger.im.service.sendmsg.MessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单聊逻辑
 *
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

    @Resource
    MessageStoreService messageStoreService;

    /** 线程池优化单聊消息处理逻辑 */
    private final ThreadPoolExecutor threadPoolExecutor;

    {
        final AtomicInteger num = new AtomicInteger(0);
        threadPoolExecutor = new ThreadPoolExecutor(8, 8, 60, TimeUnit.SECONDS,
                // 任务队列存储超过核心线程数的任务
                new LinkedBlockingDeque<>(1000), r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("[P2P] message-process-thread-" + num.getAndIncrement());
            return thread;
        });
    }

    public void processor(MessageContent messageContent) {
        // 日志打印
        log.info("消息 ID [{}] 开始处理", messageContent.getMessageId());

        threadPoolExecutor.execute(() -> {
            // 1 消息持久化落库(MQ 异步)
            messageStoreService.storeP2PMessage(messageContent);
            // 2. 返回应答报文 ACK 给自己
            ack(messageContent, ResponseVO.successResponse());
            // 3. 发送消息，同步发送方多端设备
            syncToSender(messageContent);
            // 4. 发送消息给对方所有在线端(TODO 离线端也要做消息同步)
            dispatchMessage(messageContent);
        });

        log.info("消息 ID [{}] 处理完成", messageContent.getMessageId());
    }

    public SendMessageResp send(SendMessageReq req) {

        SendMessageResp sendMessageResp = new SendMessageResp();
        MessageContent message = new MessageContent();
        message.setAppId(req.getAppId());
        message.setClientType(req.getClientType());
        message.setImei(req.getImei());
        message.setMessageId(req.getMessageId());
        message.setFromId(req.getFromId());
        message.setToId(req.getToId());
        message.setMessageBody(req.getMessageBody());
        message.setMessageTime(req.getMessageTime());

        //插入数据
        messageStoreService.storeP2PMessage(message);
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
     * 2. 发送方和接收方是否是好友
     *
     * @param fromId
     * @param toId
     * @param appId
     * @return
     */
    public ResponseVO serverPermissionCheck(String fromId, String toId, Integer appId) {
        ResponseVO responseVO = checkSendMessageService.checkSenderForbidAndMute(fromId, appId);
        if (!responseVO.isOk()) {
            return responseVO;
        }
        responseVO = checkSendMessageService.checkFriendShip(fromId, toId, appId);
        return responseVO;
    }

    /**
     * ACK 应答报文包装和发送
     *
     * @param messageContent
     * @param responseVO
     */
    public void ack(MessageContent messageContent, ResponseVO responseVO) {
        log.info("[P2P] msg ack, msgId = {}, checkResult = {}", messageContent.getMessageId(), responseVO.getCode());

        // ack 包塞入消息 id，告知客户端端 该条消息已被成功接收
        ChatMessageAck chatMessageAck = new ChatMessageAck(messageContent.getMessageId());
        responseVO.setData(chatMessageAck);
        // 发送消息，回传给发送方端
        messageProducer.sendToUserOneClient(messageContent.getFromId(),
                MessageCommand.MSG_ACK, responseVO, messageContent);
    }

    /**
     * 消息同步【发送方除本端所有端消息同步】
     *
     * @param messageContent
     */
    public void syncToSender(MessageContent messageContent) {
        log.info("[P2P] 发送方消息同步");
        messageProducer.sendToUserExceptClient(
                messageContent.getFromId(),
                MessageCommand.MSG_P2P,
                messageContent, messageContent
        );
    }

    /**
     * [单聊] 消息发送【接收端所有端都需要接收消息】
     *
     * @param messageContent
     * @return
     */
    public List<ClientInfo> dispatchMessage(MessageContent messageContent) {
        List<ClientInfo> clientInfos = messageProducer.sendToUserAllClient(
                messageContent.getToId(),
                MessageCommand.MSG_P2P,
                messageContent,
                messageContent.getAppId()
        );
        return clientInfos;
    }

}
