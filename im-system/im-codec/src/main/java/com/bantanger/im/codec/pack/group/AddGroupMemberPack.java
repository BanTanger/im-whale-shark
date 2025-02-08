package com.bantanger.im.codec.pack.group;

import lombok.Data;

import java.util.List;

/**
 * 群内添加群成员通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class AddGroupMemberPack {

    private String groupId;

    private List<String> members;

}
