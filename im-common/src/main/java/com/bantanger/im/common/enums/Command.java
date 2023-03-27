package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:54
 */
public enum Command implements CodeAdapter {
    /**
     * 心跳 9999 --> 0x270f
     */
    PING(0x270f),
    /**
     * 登录 9000 --> 0x2328
     */
    LOGIN(0x2328),
    /**
     * 登出 9003 --> 0x232b
     */
    LOGOUT(0x232b),
    /**
     * 下线通知 用于多端互斥 9002 --> 0x232a
     */
    MUTALOGIN(0x232a),

    ;


    private Integer command;

    Command(Integer command) {
        this.command = command;
    }

    @Override
    public Integer getCode() {
        return command;
    }

}