package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 审批好友申请通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class ApproverFriendRequestPack {

    private Long id;

    //1同意 2拒绝
    private Integer status;

    private Long sequence;

}
