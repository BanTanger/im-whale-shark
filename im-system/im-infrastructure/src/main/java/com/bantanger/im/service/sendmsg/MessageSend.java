package com.bantanger.im.service.sendmsg;

import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.UserSession;

/**
 * 只提供最基层的消息发送
 * @author BanTanger 半糖
 * @Date 2023/3/31 23:21
 */
public interface MessageSend {

    /**
     * 发送消息数据包给 TCP 网关
     * @param toId
     * @param command
     * @param msg
     * @param session
     * @return
     */
    boolean sendMessage(String toId, Command command, Object msg, UserSession session);

}
