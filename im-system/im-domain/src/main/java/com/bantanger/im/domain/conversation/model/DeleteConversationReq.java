package com.bantanger.im.domain.conversation.model;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 16:22
 */
@Data
public class DeleteConversationReq extends RequestBase {

    @NotBlank(message = "会话 Id 不能为空")
    private String conversationId;

    @NotBlank(message = "fromId 不能为空")
    private String fromId;

}
