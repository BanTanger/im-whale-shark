package com.bantanger.im.common.enums.command;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:54
 */
public enum SystemCommand implements Command {
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

    /**
     * 消息发送 9005 --> 0x232d
     */
    SENDMSG(0x232d)

    ;

    private Integer command;

    SystemCommand(Integer command) {
        this.command = command;
    }

    @Override
    public Integer getCommand() {
        return command;
    }

}