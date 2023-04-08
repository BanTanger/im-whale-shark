package com.bantanger.im.domain.message.service.sync;

import com.bantanger.im.codec.pack.message.MessageReadPack;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.message.MessageReceiveAckContent;
import com.bantanger.im.common.model.message.read.MessageReadContent;
import com.bantanger.im.domain.conversation.service.ConversationService;
import com.bantanger.im.service.sendmsg.MessageProducer;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
