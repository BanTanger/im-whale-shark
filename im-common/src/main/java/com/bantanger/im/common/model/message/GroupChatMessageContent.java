package com.bantanger.im.common.model.message;

import lombok.Data;

import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 11:22
 */
@Data
public class GroupChatMessageContent extends MessageContent {

    private String groupId;

    private List<String> memberId;

}
