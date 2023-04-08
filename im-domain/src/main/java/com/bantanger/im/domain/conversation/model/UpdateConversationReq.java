package com.bantanger.im.domain.conversation.model;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:22
 */
@Data
public class UpdateConversationReq extends RequestBase {

    private String conversationId;

    private Integer isMute;

    private Integer isTop;

    private String fromId;

}
