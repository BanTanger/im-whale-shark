package com.bantanger.im.codec.proto;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 11:30
 */
@Data
public class ChatMessageAck {

    private String messageId;
    private Long messageSequence;

    public ChatMessageAck(String messageId) {
        this.messageId = messageId;
    }

    public ChatMessageAck(String messageId, Long messageSequence) {
        this.messageId = messageId;
        this.messageSequence = messageSequence;
    }

}
