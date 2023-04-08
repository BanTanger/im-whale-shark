package com.bantanger.im.common.enums.conversation;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 14:26
 */
public enum ConversationTypeEnum {

    /**
     * 0 单聊 1群聊 2机器人 3公众号
     */
    P2P(0),

    GROUP(1),

    ROBOT(2),

    ;

    private Integer code;

    ConversationTypeEnum(int code){
        this.code=code;
    }

    public Integer getCode() {
        return code;
    }

}
