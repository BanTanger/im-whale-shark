package com.bantanger.im.service.strategy.command.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.message.CheckSendMessageReq;
import com.bantanger.im.infrastructure.rabbitmq.publish.MqMessageProducer;
import com.bantanger.im.service.feign.FeignMessageService;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import io.netty.channel.ChannelHandlerContext;

import static com.bantanger.im.common.constant.Constants.MsgPackConstants.*;

/**
 * TCP 层校验消息发送方合法性
 * @author BanTanger 半糖
 * @Date 2023/4/5 20:06
 */
public class GroupMsgCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(CommandExecution commandExecution) {
        ChannelHandlerContext ctx = commandExecution.getCtx();
        Message msg = commandExecution.getMsg();
        FeignMessageService feignMessageService = commandExecution.getFeignMessageService();

        CheckSendMessageReq req = CheckSendMessageReq.builder()
            .appId(msg.getMessageHeader().getAppId())
            .command(msg.getMessageHeader().getCommand())
            .build();
        JSONObject jsonObject = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));
        String fromId = jsonObject.getString(FROM_ID);
        String groupId = jsonObject.getString(GROUP_ID);
        req.setFromId(fromId);
        req.setToId(groupId);

        // 1.调用业务层校验消息发送方的内部接口
        ResponseVO responseVO = feignMessageService.checkGroupSendMessage(req);
        if (responseVO.isOk()) {
            // 2. 如果成功就投递到 MQ
            MqMessageProducer.sendMessage(msg);
        } else {
            // 3. 如果失败就发送 ACK 失败响应报文
            ChatMessageAck chatMessageAck = new ChatMessageAck(jsonObject.getString(MSG_ID));
            responseVO.setData(chatMessageAck);
            MessagePack<ResponseVO> ack = new MessagePack<>();
            ack.setData(responseVO);
            ack.setCommand(GroupEventCommand.GROUP_MSG_ACK.getCommand());
            ctx.channel().writeAndFlush(ack);
        }

    }

}