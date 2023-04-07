package com.bantanger.im.common.model.message;

import com.bantanger.im.common.model.ClientInfo;
import lombok.Data;

/**
 * 消息确认收到 ACK
 * @author BanTanger 半糖
 * @Date 2023/4/6 23:03
 */
@Data
public class MessageReceiveAckPack extends ClientInfo {

    /** 消息唯一标识 */
    private Long messageKey;

    private String fromId;

    private String toId;

    private Long messageSequence;

    /** 是否为服务端发送的消息 */
    private boolean serverSend = false;

}
