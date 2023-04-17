package com.bantanger.im.tcp.server;

import com.bantanger.im.codec.MessageDecoderHandler;
import com.bantanger.im.codec.MessageEncoderHandler;
import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.tcp.handler.HeartBeatHandler;
import com.bantanger.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/24 15:25
 */
@Slf4j
public class ImServer {

    private ImBootstrapConfig.TcpConfig config;

    private NioEventLoopGroup mainGroup;
    private NioEventLoopGroup subGroup;
    private ServerBootstrap bootstrap;

    public ImServer(ImBootstrapConfig.TcpConfig config) {
        this.config = config;
        // 创建主从线程组
        mainGroup = new NioEventLoopGroup(config.getBossThreadSize());
        subGroup = new NioEventLoopGroup(config.getWorkThreadSize());
        bootstrap = new ServerBootstrap();
        bootstrap.group(mainGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                // 服务端可连接的最大队列数量
                .option(ChannelOption.SO_BACKLOG, 10240)
                // 允许重复使用本地地址和端口
                .option(ChannelOption.SO_REUSEADDR, true)
                // 子线程组禁用 Nagle 算法，简单点说是否批量发送数据 true关闭 false开启。 开启的话可以减少一定的网络开销，但影响消息实时性
                .childOption(ChannelOption.TCP_NODELAY, true)
                // 保活机制，2h 没数据会发送心跳包检测
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        // 消息编码
                        ch.pipeline().addLast(new MessageDecoderHandler());
                        // 消息解码
                        ch.pipeline().addLast(new MessageEncoderHandler());
                        // 心跳检测 保活
                        ch.pipeline().addLast(new IdleStateHandler(
                                0, 0, 1));
                        ch.pipeline().addLast(new HeartBeatHandler(config.getHeartBeatTime()));
                        // 用户逻辑执行
                        ch.pipeline().addLast(new NettyServerHandler(config.getBrokerId(), config.getLogicUrl()));
                    }
                });
    }

    public void start() {
        // 启动服务端
        this.bootstrap.bind(config.getTcpPort());
        log.info("tcp start success");
    }

}
