package com.bantanger.im.service.strategy.command.system;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.feign.FeignMessageService;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.strategy.command.model.CommandExecutionRequest;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.ChannelHandlerContext;

/**
 * 用户登出逻辑
 * @author BanTanger 半糖
 * @Date 2023/3/25 11:56
 */
public class LogoutCommand extends BaseCommandStrategy {
    @Override
    public void systemStrategy(CommandExecutionRequest commandExecutionRequest) {
        ChannelHandlerContext ctx = commandExecutionRequest.getCtx();
        UserChannelRepository.remove(ctx.channel());
    }

}
