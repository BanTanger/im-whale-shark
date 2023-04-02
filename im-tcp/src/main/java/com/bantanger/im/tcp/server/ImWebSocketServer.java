package com.bantanger.im.tcp.server;

import com.bantanger.im.codec.WebSocketMessageDecoderHandler;
import com.bantanger.im.codec.WebSocketMessageEncoderHandler;
import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.tcp.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket 网络通讯, HTTP 包装的协议，双向通信，降低延时
 * 核心在于其定义的 Handler
 *
 * @author BanTanger 半糖
 * @Date 2023/3/24 15:25
 */
@Slf4j
public class ImWebSocketServer {

    private ImBootstrapConfig.TcpConfig config;

    private NioEventLoopGroup mainGroup;
    private NioEventLoopGroup subGroup;
    private ServerBootstrap bootstrap;

    public ImWebSocketServer(ImBootstrapConfig.TcpConfig config) {
        this.config = config;
        // 创建主从线程组
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
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
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // websocket 基于http协议，所以要有http编解码器
                        pipeline.addLast("http-codec", new HttpServerCodec());
                        // 对写大数据流的支持
                        pipeline.addLast("http-chunked", new ChunkedWriteHandler());
                        // 几乎在netty中的编程，都会使用到此hanler
                        pipeline.addLast("aggregator", new HttpObjectAggregator(65535));
                        /**
                         * websocket 服务器处理的协议，用于指定给客户端连接访问的路由 : /ws
                         * 本handler会帮你处理一些繁重的复杂的事
                         * 会帮你处理握手动作： handshaking（close, ping, pong） ping + pong = 心跳
                         * 对于websocket来讲，都是以frames进行传输的，不同的数据类型对应的frames也不同
                         */
                        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
                        pipeline.addLast(new WebSocketMessageDecoderHandler());
                        pipeline.addLast(new WebSocketMessageEncoderHandler());
                        pipeline.addLast(new NettyServerHandler(config.getBrokerId()));
                    }
                });
    }

    public void start() {
        // 启动服务端
        this.bootstrap.bind(config.getWebSocketPort());
//        logger.info("web start success");
    }

}
