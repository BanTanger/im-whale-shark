package com.bantanger.im.codec.pack.group;

import lombok.Data;

/**
 * 群成员禁言通知报文
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
@Data
public class GroupMemberSpeakPack {

    private String groupId;

    private String memberId;

    private Long speakDate;

}
