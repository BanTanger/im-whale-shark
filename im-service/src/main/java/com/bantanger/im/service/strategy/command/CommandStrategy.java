package com.bantanger.im.service.strategy.command;


import com.bantanger.im.service.strategy.command.model.CommandExecutionRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:49
 */
public interface CommandStrategy {

    /**
     * 系统命令执行策略接口
     * @param commandExecutionRequest
     */
    void systemStrategy(CommandExecutionRequest commandExecutionRequest);

}
