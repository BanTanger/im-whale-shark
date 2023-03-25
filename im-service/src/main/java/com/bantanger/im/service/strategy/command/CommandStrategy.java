package com.bantanger.im.service.strategy.command;

import com.bantanger.im.codec.proto.Message;
import io.netty.channel.ChannelHandlerContext;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 9:49
 */
public interface CommandStrategy {

    void doStrategy(ChannelHandlerContext ctx, Message msg);

}
