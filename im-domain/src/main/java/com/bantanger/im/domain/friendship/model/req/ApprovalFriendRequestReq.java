package com.bantanger.im.domain.friendship.model.req;

import com.bantanger.im.common.model.RequestBase;
import lombok.Data;


@Data
public class ApprovalFriendRequestReq extends RequestBase {

    private Long id;

    /**
     * 1同意 2拒绝
     */
    private Integer status;

}
