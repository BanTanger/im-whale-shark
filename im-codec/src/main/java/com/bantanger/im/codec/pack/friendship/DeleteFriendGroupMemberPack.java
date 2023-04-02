package com.bantanger.im.codec.pack.friendship;

import lombok.Data;

import java.util.List;

/**
 * 删除好友分组成员通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class DeleteFriendGroupMemberPack {

    public String fromId;

    private String groupName;

    private List<String> toIds;

    /** 序列号*/
    private Long sequence;

}
