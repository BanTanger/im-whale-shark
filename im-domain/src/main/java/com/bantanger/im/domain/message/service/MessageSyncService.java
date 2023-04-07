package com.bantanger.im.domain.message.service;

import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.common.model.message.MessageReceiveAckPack;
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
public class MessageSyncService {

    @Resource
    MessageProducer messageProducer;

    /**
     * 在线目标用户同步接收消息确认
     * @param pack
     */
    public void receiveMark(MessageReceiveAckPack pack) {
        // 确认接收 ACK 发送给在线目标用户全端
        messageProducer.sendToUserAllClient(pack.getToId(),
                MessageCommand.MSG_RECEIVE_ACK, pack, pack.getAppId());
    }

}
