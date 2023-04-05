package com.bantanger.im.domain.message.mq;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.common.model.message.MessageContent;
import com.bantanger.im.domain.message.service.P2PMessageService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 单聊消息接收器
 *
 * @author BanTanger 半糖
 * @Date 2023/4/3 16:19
 */
@Slf4j
@Component
public class P2PChatOperateReceiver extends AbstractChatOperateReceiver {

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
        process(message, headers, channel);
    }

    @Override
    protected void doStrategy(Integer command, JSONObject jsonObject) {
        if (MessageCommand.MSG_P2P.getCommand().equals(command)) {
            // 处理消息
            MessageContent messageContent = jsonObject.toJavaObject(MessageContent.class);
            p2pMessageService.processor(messageContent);
        }
    }

}
