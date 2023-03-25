package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.service.strategy.command.CommandStrategy;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 10:20
 */
public class CommandFactory extends CommandFactoryConfig {

    public CommandStrategy getCommandStrategy(Integer command) {
        return CommandFactoryConfig.commandStrategyMap.get(command);
    }

}
