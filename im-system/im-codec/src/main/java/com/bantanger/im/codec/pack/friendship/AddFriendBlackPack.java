package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 用户添加黑名单以后 tcp 通知数据包
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class AddFriendBlackPack {

    private String fromId;

    private String toId;

    private Long sequence;

}
