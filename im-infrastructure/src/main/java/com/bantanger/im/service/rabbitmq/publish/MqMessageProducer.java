package com.bantanger.im.service.rabbitmq.publish;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.CommandType;
import com.bantanger.im.service.rabbitmq.MqFactory;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 22:53
 */
@Slf4j
public class MqMessageProducer {

    public void sendMessage(Message message, Integer command) {
        Channel channel = null;
        String num = command.toString();
        String substring = num.substring(0, 1);
        CommandType commandType = CommandType.getCommandType(substring);
        String channelName = null;

        assert commandType != null;
        if (commandType.equals(CommandType.MESSAGE)) {
            channelName = Constants.RabbitmqConstants.Im2MessageService;
        } else if (commandType.equals(CommandType.GROUP)) {
            channelName = Constants.RabbitmqConstants.Im2GroupService;
        }

        try {
            channel = MqFactory.getChannel(channelName);

            // 解析私有协议的内容
            JSONObject o = (JSONObject) JSON.toJSON(message.getMessagePack());
            o.put("command", command);
            o.put("clientType", message.getMessageHeader().getClientType());
            o.put("imei", message.getMessageHeader().getImei());
            o.put("appId", message.getMessageHeader().getAppId());

            // TODO 开启镜像队列防止 MQ 丢失数据
            channel.basicPublish(channelName, "",
                    null, o.toJSONString().getBytes());
        } catch (Exception e) {
            log.error("发送消息出现异常：{}", e.getMessage());
        }
    }

}
