package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.common.enums.Command;
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
        commandStrategyMap.put(Command.LOGIN.getCode(), new LoginCommand());
        commandStrategyMap.put(Command.LOGOUT.getCode(), new LogoutCommand());
        commandStrategyMap.put(Command.PING.getCode(), new PingCommand());
    }

}
