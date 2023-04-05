package com.bantanger.im.domain.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:02
 */
@Data
@TableName("im_message_body")
public class ImMessageBodyEntity {

    private Integer appId;

    /** messageBody 消息实体唯一 ID 标识 */
    private Long messageKey;

    private String messageBody;

    /** 消息加密密钥，防止消息被黑客截取 */
    private String securityKey;

    private Long messageTime;

    private Long createTime;

    private String extra;

    private Integer delFlag;

}
