package com.bantanger.im.service.rabbitmq.publish;

import com.alibaba.fastjson2.JSONObject;
import com.bantanger.im.service.utils.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 22:53
 */
@Slf4j
public class MqMessageProducer {

    public static void sendMessage(Object message) {
        Channel channel = null;
        String channelName = "";
        try {
            channel = MqFactory.getChannel(channelName);
            channel.basicPublish(channelName, "",
                    null, JSONObject.toJSONString(message).getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }

}
