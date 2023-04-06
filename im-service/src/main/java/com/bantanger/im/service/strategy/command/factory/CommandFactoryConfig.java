package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.enums.command.SystemCommand;
import com.bantanger.im.service.strategy.command.message.GroupMsgCommand;
import com.bantanger.im.service.strategy.command.message.P2PMsgCommand;
import com.bantanger.im.service.strategy.command.model.CommandExecutionRequest;
import com.bantanger.im.service.strategy.command.system.PingCommand;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.system.LoginCommand;
import com.bantanger.im.service.strategy.command.system.LogoutCommand;

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

    public static void init() {
        // 系统命令策略
        commandStrategyMap.put(SystemCommand.LOGIN.getCommand(), new LoginCommand());
        commandStrategyMap.put(SystemCommand.LOGOUT.getCommand(), new LogoutCommand());
        commandStrategyMap.put(SystemCommand.PING.getCommand(), new PingCommand());
        // 消息命令策略
        commandStrategyMap.put(MessageCommand.MSG_P2P.getCommand(), new P2PMsgCommand());
        commandStrategyMap.put(GroupEventCommand.MSG_GROUP.getCommand(), new GroupMsgCommand());
    }

}
