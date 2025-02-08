package com.bantanger.im.domain.conversation.model;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @author BanTanger 半糖
 * @date 2024/1/25 23:27
 */
@Data
public class CreateConversationReq {

    private Integer appId;

    /** 会话类型 */
    @NotBlank(message = "会话类型不能为空")
    private Integer conversationType;

    @NotBlank(message = "fromId 不能为空")
    private String fromId;

    /** 目标对象 Id 或者群组 Id */
    @NotBlank(message = "toId 不能为空")
    private String toId;

}
