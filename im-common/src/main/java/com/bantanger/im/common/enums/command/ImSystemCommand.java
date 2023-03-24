package com.bantanger.im.common.enums.command;

import com.bantanger.im.common.enums.CodeAdapter;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:54
 */
public enum ImSystemCommand implements CodeAdapter {
    /**
     * 登录 9000 --> 0x2328
     */
    COMMAND_LOGIN(0x2328);

    private Integer command;

    ImSystemCommand(Integer command) {
        this.command = command;
    }

    @Override
    public Integer getCode() {
        return command;
    }

}