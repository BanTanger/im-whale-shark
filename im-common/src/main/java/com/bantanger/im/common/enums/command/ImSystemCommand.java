package com.bantanger.im.common.enums.command;

import com.bantanger.im.common.enums.CodeAdapter;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:54
 */
public enum ImSystemCommand implements CodeAdapter {
    /**
     * 心跳 9999 --> 0x270f
     */
    COMMAND_PING(0x270f),
    /**
     * 登录 9000 --> 0x2328
     */
    COMMAND_LOGIN(0x2328),
    /**
     * 登出 9003 --> 0x232b
     */
    COMMAND_LOGOUT(0x232b);

    private Integer command;

    ImSystemCommand(Integer command) {
        this.command = command;
    }

    @Override
    public Integer getCode() {
        return command;
    }

}