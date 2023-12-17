package com.bantanger.im.service.strategy.command.message.extensionpost.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.ChatMessageAck;
import com.bantanger.im.codec.proto.MessagePack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.command.MessageCommand;
import com.bantanger.im.service.rabbitmq.publish.MqMessageProducer;
import com.bantanger.im.service.strategy.command.message.extensionpost.CheckLegalMsgPostProcessor;
import com.bantanger.im.service.strategy.command.message.extensionpost.FeignDataBO;
import com.bantanger.im.service.utils.BeanPostProcessorUtil;
import com.bantanger.im.service.support.postprocessor.PostContext;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import static com.bantanger.im.common.constant.Constants.MsgPackConstants.MSG_ID;

/**
 * 校验消息是否合法的后继通知扩展点
 * @author BanTanger 半糖
 * @Date 2023/12/17 0:15
 */
@Configuration
public class NoticeCheckResPostProcessor implements CheckLegalMsgPostProcessor {

    /**
     * 功能执行后的扩展功能执行 <br>
     *
     * @param postContext
     */
    @Override
    public void handleAfter(PostContext<FeignDataBO> postContext) {
        FeignDataBO feignDataBO = postContext.getBizData();
        JSONObject msgPack = JSON.parseObject(
                JSONObject.toJSONString(feignDataBO.getMsg().getMessagePack()));
        ResponseVO responseVO = (ResponseVO) postContext.getMainProcessRes();

        if (responseVO.isOk()) {
            // 如果成功就投递到 MQ
            MqMessageProducer.sendMessage(feignDataBO.getMsg());
        } else {
            // 如果失败就发送 ACK 失败响应报文
            ChatMessageAck chatMessageAck = new ChatMessageAck(msgPack.getString(MSG_ID));
            responseVO.setData(chatMessageAck);
            MessagePack<ResponseVO> ack = new MessagePack<>();
            ack.setData(responseVO);
            ack.setCommand(MessageCommand.MSG_ACK.getCommand());
            feignDataBO.getCtx().channel().writeAndFlush(ack);
        }
    }

    /**
     * 扩展点有多个，通过优先级来管理执行顺序
     *
     * @return
     */
    @Override
    public int getPriority() {
        // 优先级最大, 确保通知到位（实际上这里优先级或小或大都不影响语义）
        return Integer.MAX_VALUE;
    }

    /**
     * 添加扩展点
     */
    @PostConstruct
    public void addPostProcessor() {
        BeanPostProcessorUtil.addPostProcessor(CheckLegalMsgPostProcessor.class, this);
    }

}
