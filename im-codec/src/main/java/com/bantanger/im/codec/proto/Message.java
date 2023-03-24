package com.bantanger.im.codec.proto;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 18:59
 */
@Data
public class Message {

    private MessageHeader messageHeader;

    private Object messagePack;

    @Override
    public String toString() {
        return "Message{" +
                "messageHeader=" + messageHeader +
                ", messagePack=" + messagePack +
                '}';
    }

}
