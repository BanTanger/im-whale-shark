package com.bantanger.im.domain.message.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.domain.message.model.MessageContent;
import com.bantanger.im.domain.message.service.P2PMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/3 16:19
 */
@Slf4j
@Component
public class ChatOperateReceiver {

    @Resource
    P2PMessageService p2pMessageService;

    @RabbitListener(
            bindings = @QueueBinding(
                    // 绑定 MQ 队列
                    value = @Queue(value = Constants.RabbitmqConstants.Im2MessageService, durable = "true"),
                    // 绑定 MQ 交换机
                    exchange = @Exchange(value = Constants.RabbitmqConstants.Im2MessageService, durable = "true")
            ),
            concurrency = "1" // 一次读取 MQ 队列中 1 条消息
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws Exception {
        // TODO 逻辑待完善
        String msg = new String(message.getBody(), "utf-8");
        log.info("MQ 队列 QUEUE 读取到消息 ::: [{}]", msg);
        Long deliveryTag = (Long) headers.get(AmqpHeaders.DELIVERY_TAG);
        try {
            JSONObject jsonObject = JSON.parseObject(msg);
            Integer command = jsonObject.getInteger("command");
            if (MessageCommand.MSG_P2P.getCommand().equals(command)) {
                // 处理消息
                MessageContent messageContent = jsonObject.toJavaObject(MessageContent.class);
                p2pMessageService.processor(messageContent);
            }
        } catch (Exception e) {
            log.error("处理消息出现异常: {}", e.getMessage());
            log.error("RMQ_CHAT_TRAN_ERROR ", e);
            log.error("NACK_MSG: {}", msg);
            //第一个false 表示不批量拒绝，第二个false表示不重回队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

}
