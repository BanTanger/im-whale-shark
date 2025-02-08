package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

import java.util.List;

/**
 * 好友分组添加成员通知包
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class AddFriendGroupMemberPack {

    public String fromId;

    private String groupName;

    private List<String> toIds;

    /** 序列号*/
    private Long sequence;

}
