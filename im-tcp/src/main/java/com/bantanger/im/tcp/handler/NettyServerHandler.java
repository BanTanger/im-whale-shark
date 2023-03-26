package com.bantanger.im.tcp.handler;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.factory.CommandFactory;
import com.bantanger.im.service.utils.UserChannelRepository;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:23
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = parseCommand(msg);
        CommandFactory commandFactory = new CommandFactory();
        CommandStrategy commandStrategy = commandFactory.getCommandStrategy(command);
        commandStrategy.doStrategy(ctx, msg);
    }

    protected Integer parseCommand(Message msg) {
        return msg.getMessageHeader().getCommand();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        UserChannelRepository.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UserChannelRepository.remove(ctx.channel());
//        logger.info("剩余通道个数：{}", UserChannelRepository.CHANNEL_GROUP.size());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        UserChannelRepository.remove(ctx.channel());
    }

}
