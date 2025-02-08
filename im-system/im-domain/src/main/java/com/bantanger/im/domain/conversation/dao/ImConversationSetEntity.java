package com.bantanger.im.domain.conversation.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:22
 */
@Data
@TableName("im_conversation_set")
public class ImConversationSetEntity {

    /** 会话id 0_fromId_toId */
    private String conversationId;

    /** 会话类型 */
    private Integer conversationType;

    private String fromId;

    /** 目标对象 Id 或者群组 Id */
    private String toId;

    /** 是否禁言 */
    private int isMute;

    /** 是否置顶消息 */
    private int isTop;

    private Long sequence;

    /** 消息已读偏序 */
    private Long readSequence;

    private Integer appId;

}
