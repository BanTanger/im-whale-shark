package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/26 20:26
 */
public enum DeviceMultiLoginEnum implements CodeAdapter {

    /**
     * 单端登录 仅允许 Windows、Web、Android 或 iOS 单端登录。
     */
    ONE(1,"单端登录"),

    /**
     * 双端登录 允许 Windows、Mac、Android 或 iOS 单端登录，同时允许与 Web 端同时在线。
     */
    TWO(2,"双端登录"),

    /**
     * 三端登录 允许 Android 或 iOS 单端登录(互斥)，Windows 或者 Mac 单聊登录(互斥)，同时允许 Web 端同时在线
     */
    THREE(3,"三端登录"),

    /**
     * 多端同时在线 允许 Windows、Mac、Web、Android 或 iOS 多端或全端同时在线登录
     */
    ALL(4,"多端同时在线");

    private Integer loginMode;
    private String loginDesc;

    DeviceMultiLoginEnum(Integer loginMode, String loginDesc) {
        this.loginMode = loginMode;
        this.loginDesc = loginDesc;
    }

    @Override
    public Integer getCode() {
        return loginMode;
    }

}
