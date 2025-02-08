package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 已读好友申请通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class ReadAllFriendRequestPack {

    private String fromId;

    private Long sequence;

}
