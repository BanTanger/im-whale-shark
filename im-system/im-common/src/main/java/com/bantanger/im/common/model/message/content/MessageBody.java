package com.bantanger.im.common.model.message.content;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 11:22
 */
@Data
public class MessageBody {
    private Integer appId;

    /** messageBodyId*/
    private Long messageKey;

    /** messageBody*/
    private String messageBody;

    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;
}
