package com.bantanger.im.message.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 13:02
 */
@Data
@TableName("im_message_history")
public class ImMessageHistoryEntity {

    private Integer appId;

    private String fromId;

    private String toId;

    /** 消息拥有者，写扩散标识写入哪个消息队列 */
    private String ownerId;

    /** messageBody 消息实体唯一 ID 标识 */
    private Long messageKey;

    /** 序列号 */
    private Long sequence;

    private String messageRandom;

    private Long messageTime;

    private Long createTime;

}
