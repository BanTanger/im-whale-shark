package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

/**
 * 删除好友分组通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class DeleteFriendGroupPack {

    public String fromId;

    private String groupName;

    /** 序列号*/
    private Long sequence;

}
