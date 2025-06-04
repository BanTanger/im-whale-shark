package com.bantanger.im.common.enums.error;


import com.bantanger.im.common.exception.ApplicationExceptionEnum;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 16:43
 */
public enum ConversationErrorCode implements ApplicationExceptionEnum {

    CONVERSATION_UPDATE_PARAM_ERROR(50000,"会话参数修改错误"),
    CONVERSATION_CREATE_FAIL(50001, "会话创建失败"),

    ;

    private int code;
    private String error;

    ConversationErrorCode(int code, String error){
        this.code = code;
        this.error = error;
    }
    @Override
    public int getCode() {
        return this.code;
    }

    @Override
    public String getError() {
        return this.error;
    }

}
