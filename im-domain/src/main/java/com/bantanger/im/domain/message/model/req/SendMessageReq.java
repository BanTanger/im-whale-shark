package com.bantanger.im.domain.message.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 17:06
 */
@Data
public class SendMessageReq extends RequestBase {

    //客户端传的messageId
    private String messageId;

    private String fromId;

    private String toId;

    private int messageRandom;

    private long messageTime;

    private String messageBody;
    /**
     * 这个字段缺省或者为 0 表示需要计数，为 1 表示本条消息不需要计数，即右上角图标数字不增加
     */
    private int badgeMode;

    private Long messageLifeTime;

    private Integer appId;

}
