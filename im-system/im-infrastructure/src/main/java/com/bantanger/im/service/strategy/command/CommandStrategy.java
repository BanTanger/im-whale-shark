package com.bantanger.im.service.strategy.command;


import com.bantanger.im.service.strategy.command.model.CommandExecution;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:49
 */
public interface CommandStrategy {

    /**
     * 系统命令执行策略接口
     * @param commandExecution
     */
    void systemStrategy(CommandExecution commandExecution);

}
