package com.bantanger.im.common.model.message.store;

import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.common.model.message.MessageBody;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 17:42
 */
@Data
public class DoStoreGroupMessageDto {

    private GroupChatMessageContent groupChatMessageContent;

    private MessageBody messageBody;

}
