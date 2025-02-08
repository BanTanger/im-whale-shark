package com.bantanger.im.domain.group.model.req;

import lombok.Data;

/**
 * 踢人出群通知报文
 * @author BanTanger 半糖
 */
@Data
public class RemoveGroupMemberPack {

    private String groupId;

    private String member;

}
