package com.bantanger.im.domain.friendship.model.req.callback;

import com.bantanger.im.domain.friendship.model.req.friend.FriendDto;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/30 20:07
 */
@Data
public class AddFriendAfterCallbackDto {

    private String fromId;

    private FriendDto toItem;
}
