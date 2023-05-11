package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.service.strategy.command.CommandStrategy;

/**
 * 使用单例模式防止每次读取 channel 都需要初始化 CommandFactory, 所导致的 CPU 飙升
 * @author BanTanger 半糖
 * @Date 2023/3/25 10:20
 */
public class CommandFactory extends CommandFactoryConfig {

    private CommandFactory() {
    }

    private static class SingletonHolder {
        private static final CommandFactory INSTANCE = new CommandFactory();
    }

    public static CommandFactory getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public CommandStrategy getCommandStrategy(Integer command) {
        return commandStrategyMap.get(command);
    }

}
