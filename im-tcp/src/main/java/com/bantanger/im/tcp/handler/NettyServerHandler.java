package com.bantanger.im.tcp.handler;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.factory.CommandFactory;
import com.bantanger.im.service.strategy.command.model.CommandExecutionRequest;
import com.bantanger.im.service.utils.UserChannelRepository;
import com.bantanger.im.service.feign.FeignMessageService;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:23
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private Integer brokerId;

    private String logicUrl;

    private FeignMessageService feignMessageService;

    public NettyServerHandler(Integer brokerId, String logicUrl) {
        this.brokerId = brokerId;
        feignMessageService = Feign.builder()
                .encoder(new JacksonEncoder())
                .decoder(new JacksonDecoder())
                // 设置超时时间
                .options(new Request.Options(1000, 3500))
                .target(FeignMessageService.class, logicUrl);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = parseCommand(msg);
        CommandFactory commandFactory = new CommandFactory();
        CommandStrategy commandStrategy = commandFactory.getCommandStrategy(command);

        // 使用 req 包装参数内部传参，避免后期新增参数需要扩展接口字段
        CommandExecutionRequest commandExecutionRequest = new CommandExecutionRequest();
        commandExecutionRequest.setCtx(ctx);
        commandExecutionRequest.setBrokeId(brokerId);
        commandExecutionRequest.setMsg(msg);
        commandExecutionRequest.setFeignMessageService(feignMessageService);

        // 执行策略
        commandStrategy.systemStrategy(commandExecutionRequest);
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
