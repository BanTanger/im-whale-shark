package com.bantanger.im.common.model.message.store;

import com.bantanger.im.common.model.message.content.MessageBody;
import com.bantanger.im.common.model.message.content.MessageContent;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 8:51
 */
@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private MessageBody messageBody;

}
