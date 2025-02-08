package com.bantanger.im.service.strategy.command.system;

import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.ChannelHandlerContext;

/**
 * 用户登出逻辑
 * @author BanTanger 半糖
 * @Date 2023/3/25 11:56
 */
public class LogoutCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(CommandExecution commandExecution) {
        ChannelHandlerContext ctx = commandExecution.getCtx();
        UserChannelRepository.remove(ctx.channel());
    }

}
