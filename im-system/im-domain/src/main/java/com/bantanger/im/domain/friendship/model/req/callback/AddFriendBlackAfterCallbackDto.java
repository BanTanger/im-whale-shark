package com.bantanger.im.domain.friendship.model.req.callback;

import lombok.Data;

@Data
public class AddFriendBlackAfterCallbackDto {

    private String fromId;

    private String toId;
}
