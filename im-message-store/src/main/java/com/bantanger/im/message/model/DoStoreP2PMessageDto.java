package com.bantanger.im.message.model;

import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.message.dao.ImMessageBodyEntity;
import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/6 9:24
 */
@Data
public class DoStoreP2PMessageDto {

    private MessageContent messageContent;

    private ImMessageBodyEntity imMessageBodyEntity;

}
