package com.bantanger.im.tcp.handler;

import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.service.rabbitmq.publish.MqMessageProducer;
import com.bantanger.im.service.strategy.command.CommandStrategy;
import com.bantanger.im.service.strategy.command.factory.CommandFactory;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import com.bantanger.im.service.utils.UserChannelRepository;
import com.bantanger.im.service.feign.FeignMessageService;
import feign.Feign;
import feign.Request;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 19:23
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<Message> {

    private Integer brokerId;

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

    /**
     * 采用对象池复用对象，防止在启动项目时 CPU 占用率飙升
     */
    private final GenericObjectPool<CommandExecution> commandExecutionRequestPool
            = new GenericObjectPool<>(new CommandExecutionFactory());

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception {
        Integer command = parseCommand(msg);
        CommandFactory commandFactory = CommandFactory.getInstance();
        CommandStrategy commandStrategy = commandFactory.getCommandStrategy(command);

        CommandExecution commandExecution = null;
        try {
            // 从对象池中获取 CommandExecution 对象
            commandExecution = commandExecutionRequestPool.borrowObject();
            commandExecution.setCtx(ctx);
            commandExecution.setBrokeId(brokerId);
            commandExecution.setMsg(msg);
            commandExecution.setFeignMessageService(feignMessageService);

            if (commandStrategy != null) {
                // 执行策略
                commandStrategy.systemStrategy(commandExecution);
            } else {
                MqMessageProducer.sendMessage(msg, command);
            }
        } finally {
            // 将对象归还给对象池
            if (commandExecution != null) {
                commandExecutionRequestPool.returnObject(commandExecution);
            }
        }
    }

    protected Integer parseCommand(Message msg) {
        return msg.getMessageHeader().getCommand();
    }

    /**
     * CommandExecution 对象工厂
     */
    private static class CommandExecutionFactory extends BasePooledObjectFactory<CommandExecution> {
        @Override
        public CommandExecution create() throws Exception {
            return new CommandExecution();
        }

        @Override
        public PooledObject<CommandExecution> wrap(CommandExecution obj) {
            return new DefaultPooledObject<>(obj);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        UserChannelRepository.add(ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        UserChannelRepository.forceOffLine(ctx.channel());
        ctx.close();
//        logger.info("剩余通道个数：{}", UserChannelRepository.CHANNEL_GROUP.size());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        UserChannelRepository.remove(ctx.channel());
    }

}