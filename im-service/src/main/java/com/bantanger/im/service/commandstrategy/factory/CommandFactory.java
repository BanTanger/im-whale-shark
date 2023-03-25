package com.bantanger.im.service.commandstrategy.factory;

import com.bantanger.im.service.commandstrategy.CommandStrategy;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 10:20
 */
public class CommandFactory extends CommandFactoryConfig {

    public CommandStrategy getCommandStrategy(Integer command) {
        return commandStrategyMap.get(command);
    }

}
