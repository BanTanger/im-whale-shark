package com.bantanger.im.domain.friendship.model.req.friend;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Data
public class GetFriendShipRequestReq extends RequestBase {

    @NotBlank(message = "用户id不能为空")
    private String fromId;

}
