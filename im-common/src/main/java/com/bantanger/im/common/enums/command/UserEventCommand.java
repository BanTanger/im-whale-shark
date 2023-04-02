package com.bantanger.im.common.enums.command;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/1 17:54
 */
public enum UserEventCommand implements Command {

    // 用户修改 command 4000 --> 0xfa0
    USER_MODIFY(0xfa0),

    // 用户在线状态修改 4001 --> 0xfa1
    USER_ONLINE_STATUS_CHANGE(0xfa1),

    // 用户在线状态通知报文 4004 --> 0xfa4
    USER_ONLINE_STATUS_CHANGE_NOTIFY(0xfa4),

    // 用户在线状态通知同步报文 4005 --> 0xfa5
    USER_ONLINE_STATUS_CHANGE_NOTIFY_SYNC(0xfa5),

    ;

    private int command;

    UserEventCommand(int command){
        this.command=command;
    }


    @Override
    public Integer getCommand() {
        return command;
    }

}
