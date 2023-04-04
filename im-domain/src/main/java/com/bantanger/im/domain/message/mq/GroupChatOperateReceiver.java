package com.bantanger.im.domain.message.mq;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.message.GroupChatMessageContent;
import com.bantanger.im.domain.message.service.GroupMessageService;
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
 * 群聊消息接收器
 *
 * @author BanTanger 半糖
 * @Date 2023/4/4 23:19
 */
@Slf4j
@Component
public class GroupChatOperateReceiver extends AbstractChatOperateReceiver {

    @Resource
    GroupMessageService groupMessageService;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = Constants.RabbitmqConstants.Im2GroupService, durable = "true"),
                    exchange = @Exchange(value = Constants.RabbitmqConstants.Im2GroupService, durable = "true")
            ),
            concurrency = "1"
    )
    public void onChatMessage(@Payload Message message,
                              @Headers Map<String, Object> headers,
                              Channel channel) throws Exception {
        process(message, headers, channel);
    }

    @Override
    protected void doStrategy(Integer command, JSONObject jsonObject) {
        if (command.equals(GroupEventCommand.MSG_GROUP.getCommand())) {
            //处理消息
            GroupChatMessageContent messageContent
                    = jsonObject.toJavaObject(GroupChatMessageContent.class);
            groupMessageService.processor(messageContent);
        }
    }


}
