package com.bantanger.im.common.rabbitmq.listener;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.rabbitmq.MqFactory;
import com.bantanger.im.common.rabbitmq.process.BaseProcess;
import com.bantanger.im.common.rabbitmq.process.ProcessFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 22:59
 */
@Slf4j
public class MqMessageListener {

    public static String brokerId;

    /**
     * 每一个服务器节点，都绑定一个对应的queue，格式为 MessageService2Im + brokerId（每一个服务端的唯一编号）
     */
    private static void startListenerMessage() {
        try {
            Channel channel = MqFactory.getChannel(Constants.RabbitmqConstants.MessageService2Im + brokerId);
            channel.queueDeclare(Constants.RabbitmqConstants.MessageService2Im + brokerId,
                    true, false, false, null);
            channel.queueBind(Constants.RabbitmqConstants.MessageService2Im + brokerId,
                    Constants.RabbitmqConstants.MessageService2Im, brokerId);
            channel.basicConsume(Constants.RabbitmqConstants.MessageService2Im + brokerId, false,
                    new DefaultConsumer(channel) {
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            try {
                                String msgStr = new String(body);
                                log.info("服务端监听消息信息为 {} ", msgStr);

                                // 消息写入数据通道
                                MessagePack messagePack = JSONObject.parseObject(msgStr, MessagePack.class);
                                BaseProcess messageProcess = ProcessFactory.getMessageProcess(messagePack.getCommand());
                                messageProcess.process(messagePack);

                                // 消息成功写入通道后发送应答 Ack
                                channel.basicAck(envelope.getDeliveryTag(), false);

                            } catch (Exception e) {
                                e.printStackTrace();

                                // 消息不能正常写入通道，发送失败应答 NAck
                                channel.basicNack(envelope.getDeliveryTag(), false, false);
                            }
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
