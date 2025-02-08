package com.bantanger.im.common.enums.user;

public enum UserTypeEnum {

    IM_USER(1),

    APP_ADMIN(100),
    ;

    private int code;

    UserTypeEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }

}
