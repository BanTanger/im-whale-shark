package com.bantanger.im.tcp.handler;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.commandstrategy.CommandStrategy;
import com.bantanger.im.service.commandstrategy.factory.CommandFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:23
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

//    private final static Logger logger = LoggerFactory.getLogger(NettyServerHandler.class);

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

}
