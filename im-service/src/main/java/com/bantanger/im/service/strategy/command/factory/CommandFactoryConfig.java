package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.common.enums.command.SystemCommand;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.impl.LoginCommand;
import com.bantanger.im.service.strategy.command.impl.LogoutCommand;
import com.bantanger.im.service.strategy.command.impl.PingCommand;

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
        commandStrategyMap.put(SystemCommand.LOGIN.getCommand(), new LoginCommand());
        commandStrategyMap.put(SystemCommand.LOGOUT.getCommand(), new LogoutCommand());
        commandStrategyMap.put(SystemCommand.PING.getCommand(), new PingCommand());
    }

}
