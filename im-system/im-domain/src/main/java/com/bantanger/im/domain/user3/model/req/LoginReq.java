package com.bantanger.im.domain.user3.model.req;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 8:37
 */
@Data
public class LoginReq {

    @NotNull(message = "用户 ID 不能为空")
    private String userId;

    @NotNull(message = "appId 不能为空")
    private Integer appId;

    private Integer clientType;
}
