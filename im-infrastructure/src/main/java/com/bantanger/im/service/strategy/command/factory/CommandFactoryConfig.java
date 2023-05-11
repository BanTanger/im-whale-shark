package com.bantanger.im.service.strategy.command.factory;

import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.enums.command.SystemCommand;
import com.bantanger.im.service.strategy.command.message.GroupMsgCommand;
import com.bantanger.im.service.strategy.command.message.P2PMsgCommand;
import com.bantanger.im.service.strategy.command.system.PingCommand;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.system.LoginCommand;
import com.bantanger.im.service.strategy.command.system.LogoutCommand;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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

    private static LoginCommand loginCommand = new LoginCommand();
    private static LogoutCommand logoutCommand = new LogoutCommand();
    private static PingCommand pingCommand = new PingCommand();
    private static P2PMsgCommand p2PMsgCommand = new P2PMsgCommand();
    private static GroupMsgCommand groupMsgCommand = new GroupMsgCommand();

    @PostConstruct
    public static void init() {
        // 系统命令策略
        commandStrategyMap.put(SystemCommand.LOGIN.getCommand(), loginCommand);
        commandStrategyMap.put(SystemCommand.LOGOUT.getCommand(), logoutCommand);
        commandStrategyMap.put(SystemCommand.PING.getCommand(), pingCommand);
        // 消息命令策略
        commandStrategyMap.put(MessageCommand.MSG_P2P.getCommand(), p2PMsgCommand);
        commandStrategyMap.put(GroupEventCommand.MSG_GROUP.getCommand(), groupMsgCommand);
    }

}
