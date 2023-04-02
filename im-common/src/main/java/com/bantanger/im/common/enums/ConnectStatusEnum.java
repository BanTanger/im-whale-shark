package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/31 23:06
 */
public enum ConnectStatusEnum {

    /**
     * 管道链接状态,1=在线，2=离线。。
     */
    ONLINE_STATUS(1),

    OFFLINE_STATUS(2),
    ;

    private Integer code;

    ConnectStatusEnum(Integer code){
        this.code=code;
    }

    public Integer getCode() {
        return code;
    }

}
