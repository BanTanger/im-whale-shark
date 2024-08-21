package com.bantanger.im.service.strategy.command;

import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.enums.command.SystemCommand;
import com.bantanger.im.service.strategy.command.message.GroupMsgCommand;
import com.bantanger.im.service.strategy.command.message.P2PMsgCommand;
import com.bantanger.im.service.strategy.command.system.LoginCommand;
import com.bantanger.im.service.strategy.command.system.LogoutCommand;
import com.bantanger.im.service.strategy.command.system.PingCommand;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 10:03
 */
public class CommandFactoryConfig {

    /**
     * 命令维护策略组
     */
    protected static Map<Integer, CommandStrategy> commandStrategyMap = new ConcurrentHashMap<>();

    private static final LoginCommand LOGIN_COMMAND = new LoginCommand();
    private static final LogoutCommand LOGOUT_COMMAND = new LogoutCommand();
    private static final PingCommand PING_COMMAND = new PingCommand();
//    private static final CheckLegalMsgCommand CHECK_LEGAL_MSG_COMMAND = new CheckLegalMsgCommand();
    private static final P2PMsgCommand p2PMsgCommand = new P2PMsgCommand();
    private static final GroupMsgCommand groupMsgCommand = new GroupMsgCommand();


    public static void init() {
        // 系统命令策略
        commandStrategyMap.put(SystemCommand.LOGIN.getCommand(), LOGIN_COMMAND);
        commandStrategyMap.put(SystemCommand.LOGOUT.getCommand(), LOGOUT_COMMAND);
        commandStrategyMap.put(SystemCommand.PING.getCommand(), PING_COMMAND);
        // 消息命令策略
        commandStrategyMap.put(MessageCommand.MSG_P2P.getCommand(), p2PMsgCommand);
        commandStrategyMap.put(GroupEventCommand.MSG_GROUP.getCommand(), groupMsgCommand);
    }

}
