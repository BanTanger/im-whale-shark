package com.bantanger.im.message.model;

import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.message.dao.ImMessageBodyEntity;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 12:24
 */
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private ImMessageBodyEntity imMessageBodyEntity;

}
