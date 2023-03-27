package com.bantanger.im.service.rabbitmq.listener;

import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.service.utils.MqFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;


/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 22:59
 */
@Slf4j
public class MqMessageListener {

    public static String brokerId;

    private static void startListenerMessage() {
        try {
            Channel channel = MqFactory.getChannel(Constants.RabbitmqConstants.MessageService2Im + brokerId);
            channel.queueDeclare(Constants.RabbitmqConstants.MessageService2Im + brokerId,
                    true, false, false, null);
            channel.queueBind(Constants.RabbitmqConstants.MessageService2Im + brokerId,
                    Constants.RabbitmqConstants.MessageService2Im, brokerId);
            channel.basicConsume(Constants.RabbitmqConstants.MessageService2Im, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            // TODO 处理消息服务发来的信息
                            String msgStr = new String(body);
                            log.info(msgStr);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        startListenerMessage();
    }

    public static void init(String brokerId) {
        if (StringUtils.isBlank(MqMessageListener.brokerId)) {
            MqMessageListener.brokerId = brokerId;
        }
        startListenerMessage();
    }

}
