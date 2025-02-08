package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * 踢人出群通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class RemoveGroupMemberPack {

    private String groupId;

    private String member;

}
