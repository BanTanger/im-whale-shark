package com.bantanger.im.domain.message.service.sync;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.message.MessageReceiveAckContent;
import com.bantanger.im.common.model.message.read.MessageReadContent;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 11:05
 */
public interface MessageSyncService {

    /**
     * 在线目标用户同步接收消息确认
     * 在 {@link com.bantanger.im.domain.message.mq.P2PChatOperateReceiver}
     * 和 {@link com.bantanger.im.domain.message.mq.GroupChatOperateReceiver} 里被调度
     * @param pack
     */
    void receiveMark(MessageReceiveAckContent pack);

    /**
     * 消息已读功能
     * 在 {@link com.bantanger.im.domain.message.mq.P2PChatOperateReceiver}
     * 和 {@link com.bantanger.im.domain.message.mq.GroupChatOperateReceiver} 里被调度
     * 1. 更新会话 Seq
     * 2. 通知在线同步端发送指定 command
     * 3. 发送已读回执通知原消息发送方
     * @param messageContent
     * @param notify 消息已读 TCP 通知【同步接收所有端】
     * @param receipt 消息已读回执 TCP 通知 【发送给原消息发送方】
     */
    void readMark(MessageReadContent messageContent, Command notify, Command receipt);

    /**
     * 增量拉取离线消息功能
     * @param req
     * @return
     */
    ResponseVO syncOfflineMessage(SyncReq req);
}
