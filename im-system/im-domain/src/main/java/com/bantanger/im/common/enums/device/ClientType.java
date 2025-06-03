package com.bantanger.im.common.enums.device;

import com.bantanger.im.common.enums.CodeAdapter;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 16:01
 */
public enum ClientType implements CodeAdapter {

    WEBAPI(0, "webapi"),
    WEB(1, "web"),
    IOS(2, "ios"),
    ANDROID(3, "android"),
    WINDOWS(4, "windows"),
    MAC(5, "mac"),
    ;

    private Integer code;
    private String info;

    ClientType(int code, String info) {
        this.code = code;
        this.info = info;
    }

    @Override
    public Integer getCode() {
        return this.code;
    }

    public String getInfo() {
        return info;
    }

    /**
     * 判断是否为同一类型客户端
     * @param dtoClientType 用户当前登录端信息
     * @param channelClientType 用户之前登录端信息
     * @return 是否为同一类型客户端
     */
    public static boolean isSameClient(Integer dtoClientType, Integer channelClientType) {
        if ((IOS.getCode().equals(dtoClientType) || ANDROID.getCode().equals(dtoClientType)) &&
                (IOS.getCode().equals(channelClientType) || ANDROID.getCode().equals(channelClientType))) {
            return true;
        }
        if ((MAC.getCode().equals(dtoClientType) || WINDOWS.getCode().equals(dtoClientType)) &&
                (MAC.getCode().equals(channelClientType) || WINDOWS.getCode().equals(channelClientType))) {
            return true;
        }
        return false;
    }

}
