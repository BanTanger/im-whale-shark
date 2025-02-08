package com.bantanger.im.domain.friendship.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotNull;


@Data
public class ApprovalFriendRequestReq extends RequestBase {

    @NotNull(message = "好友审批 Id 不能为空")
    private Long id;

    /**
     * 1同意 2拒绝
     */
    @NotNull(message = "审批状态不能为空")
    private Integer status;

}
