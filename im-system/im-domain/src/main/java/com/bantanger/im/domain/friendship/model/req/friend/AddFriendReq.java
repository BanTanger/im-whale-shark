package com.bantanger.im.domain.friendship.model.req.friend;


import com.bantanger.im.common.model.RequestBase;
import lombok.Data;
import lombok.ToString;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@Data
@ToString
public class AddFriendReq extends RequestBase {

    @NotBlank(message = "fromId不能为空")
    private String fromId;

    @NotNull(message = "toItem不能为空")
    private FriendDto toItem;

}
