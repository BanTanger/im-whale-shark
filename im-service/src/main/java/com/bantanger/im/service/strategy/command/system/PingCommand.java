package com.bantanger.im.service.strategy.command.system;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.AttributeKey;
import org.springframework.stereotype.Component;

/**
 * 心跳检测
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:24
 */
public class PingCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(ChannelHandlerContext ctx, Message msg, Integer brokeId) {
        /**
         * channel 绑定当前时间
         */
        ctx.channel().attr(AttributeKey.valueOf(Constants.ChannelConstants.ReadTime)).set(System.currentTimeMillis());
    }

}
