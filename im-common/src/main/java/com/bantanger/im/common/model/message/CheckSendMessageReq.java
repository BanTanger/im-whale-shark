package com.bantanger.im.common.model.message;

import lombok.Data;

/**
 * 这个 Controller 层的请求体放在 Common 层，供 TCP 层调度
 * @author BanTanger 半糖
 * @Date 2023/4/5 19:06
 */
@Data
public class CheckSendMessageReq {

    private String fromId;

    private String toId;

    private Integer appId;

    private Integer command;

}
