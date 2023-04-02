package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * 修改群成员通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class UpdateGroupMemberPack {

    private String groupId;

    private String memberId;

    private String alias;

    private String extra;

}
