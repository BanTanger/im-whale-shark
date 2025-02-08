package com.bantanger.im.common.enums;

import com.bantanger.im.common.enums.CodeAdapter;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:57
 */
public enum MessageType implements CodeAdapter {
    // 0x0. json、 0x1. protobuf、 0x2. xml
    DATA_TYPE_JSON(0x0),
    DATA_TYPE_PROTOBUF(0x1),
    DATA_TYPE_XML(0x2);

    private Integer msgType;

    MessageType(Integer msgType) {
        this.msgType = msgType;
    }

    @Override
    public Integer getCode() {
        return msgType;
    }

}
