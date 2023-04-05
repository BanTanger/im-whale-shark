package com.bantanger.im.service.sendmsg;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.Command;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import javax.annotation.Resource;
import java.util.List;

/**
 * 抽象类完成基础功能的实现，以及定制化方法的抽象定义
 * @author BanTanger 半糖
 * @Date 2023/4/1 16:04
 */
@Slf4j
public abstract class AbstractMessageSend implements MessageSend {

    @Resource
    RabbitTemplate rabbitTemplate;

    private final String queueName = Constants.RabbitmqConstants.MessageService2Im;

    @Override
    public boolean sendMessage(String toId, Command command, Object msg, UserSession session) {
        // 将具体消息以及其他关键信息头封装成数据包，指定该消息应发送的 channel 消息通道
        MessagePack<Object> messagePack = new MessagePack<>();
        messagePack.setCommand(command.getCommand());
        messagePack.setToId(toId);
        messagePack.setClientType(session.getClientType());
        messagePack.setAppId(session.getAppId());
        messagePack.setImei(session.getImei());
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(msg));
        messagePack.setData(jsonObject);

        // 将数据包转换成 JSON 对象发送出去
        String body = JSONObject.toJSONString(messagePack);
        return sendMessage(session, body);
    }

    /**
     * 具体发送逻辑
     * @param session
     * @param msg
     * @return
     */
    private boolean sendMessage(UserSession session, Object msg) {
        // 行为埋点
        try {
            log.info("send message {} ", msg);
            // MQ 发送消息
            rabbitTemplate.convertAndSend(queueName, session.getBrokerId() + "", msg);
            return true;
        } catch (AmqpException e) {
            log.error("send error {} ", e.getMessage());
            return false;
        }
    }

    /**
     * 将消息发送给所有端，用于消息同步[对自己，对他人]
     * @param toId
     * @param command
     * @param data
     * @param appId
     * @return
     */
    public abstract List<ClientInfo> sendToUserAllClient(String toId, Command command, Object data, Integer appId);

    /**
     * 将消息发送给指定端
     * @param toId
     * @param command
     * @param data
     * @param clientInfo
     */
    public abstract void sendToUserOneClient(String toId, Command command, Object data, ClientInfo clientInfo);

    /**
     * 将消息发送给除了指定端的其他端
     * @param toId
     * @param command
     * @param data
     * @param clientInfo
     */
    public abstract void sendToUserExceptClient(String toId, Command command, Object data, ClientInfo clientInfo);
}
