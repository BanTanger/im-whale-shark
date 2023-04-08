package com.bantanger.im.codec.pack.conversation;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 17:09
 */
@Data
public class UpdateConversationPack {

    private String conversationId;

    private Integer isMute;

    private Integer isTop;

    private Integer conversationType;

    private Long sequence;

}
