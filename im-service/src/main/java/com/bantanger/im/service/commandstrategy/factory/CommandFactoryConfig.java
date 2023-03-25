package com.bantanger.im.service.commandstrategy.factory;

import com.bantanger.im.common.enums.command.ImSystemCommand;
import com.bantanger.im.service.commandstrategy.CommandStrategy;
import com.bantanger.im.service.commandstrategy.impl.LoginCommand;

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
        commandStrategyMap.put(ImSystemCommand.COMMAND_LOGIN.getCode(), new LoginCommand());
    }

}
