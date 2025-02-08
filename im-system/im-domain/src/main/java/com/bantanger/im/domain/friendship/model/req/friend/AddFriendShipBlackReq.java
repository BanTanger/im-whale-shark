package com.bantanger.im.domain.friendship.model.req.friend;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;


@Data
public class AddFriendShipBlackReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String fromId;

    private String toId;
}
