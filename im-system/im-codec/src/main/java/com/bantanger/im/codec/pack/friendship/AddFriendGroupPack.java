package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 用户创建好友分组通知包
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class AddFriendGroupPack {

    public String fromId;

    private String groupName;

    /** 序列号*/
    private Long sequence;

}
