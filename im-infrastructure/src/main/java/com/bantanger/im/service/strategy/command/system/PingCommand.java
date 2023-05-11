package com.bantanger.im.service.strategy.command.system;

import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;

/**
 * 心跳检测
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:24
 */
public class PingCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(CommandExecution commandExecution) {
        ChannelHandlerContext ctx = commandExecution.getCtx();
        /**
         * channel 绑定当前时间
         */
        ctx.channel().attr(AttributeKey.valueOf(Constants.ChannelConstants.ReadTime)).set(System.currentTimeMillis());
    }

}
