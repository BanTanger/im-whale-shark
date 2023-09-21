package com.bantanger.im.service.rabbitmq;

import com.bantanger.im.codec.config.ImBootstrapConfig;
import com.bantanger.im.common.constant.Constants;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 22:39
 */
@Slf4j
public class MqFactory {

    private static ConnectionFactory factory = null;

    private static Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    private static Connection getConnection() {
        Connection connection = null;
        try {
            connection = factory.newConnection();
        } catch (IOException | TimeoutException e) {
            log.error("MQ 连接失败，错误原因:", e);
        }
        return connection;
    }

    public static Channel getChannel(String channelName) {
        Channel channel = channelMap.get(channelName);
        if (channel == null) {
            try {
                channel = getConnection().createChannel();
            } catch (IOException e) {
                log.error("MQ 创建 Channel 失败", e);
            }
            channelMap.put(channelName, channel);
        }
        return channel;
    }

    public static void init(ImBootstrapConfig.Rabbitmq rabbitmq) {
        if (factory == null) {
            factory = new ConnectionFactory();
            factory.setHost(rabbitmq.getHost());
            factory.setPort(rabbitmq.getPort());
            factory.setUsername(rabbitmq.getUserName());
            factory.setPassword(rabbitmq.getPassword());
            factory.setVirtualHost(rabbitmq.getVirtualHost());
        }
    }

    public static void createExchange() {
        try {
            Channel channel = getConnection().createChannel();
            channel.exchangeDeclare(Constants.RabbitmqConstants.MessageService2Im, "direct");
        } catch (IOException e) {
            log.error("创建交换机失败，错误原因:", e);
        }
    }
}
