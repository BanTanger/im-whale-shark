package com.bantanger.im.service.strategy.command.message;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.proto.Message;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.message.CheckSendMessageReq;
import com.bantanger.im.service.feign.FeignMessageService;
import com.bantanger.im.service.strategy.command.BaseCommandStrategy;
import com.bantanger.im.service.strategy.command.message.extensionpost.CheckLegalMsgPostProcessor;
import com.bantanger.im.service.strategy.command.message.extensionpost.FeignDataBO;
import com.bantanger.im.service.strategy.command.model.CommandExecution;
import com.bantanger.im.service.support.postprocessor.PostContext;
import com.bantanger.im.service.support.postprocessor.PostProcessorContainer;
import lombok.extern.slf4j.Slf4j;

import static com.bantanger.im.common.constant.Constants.MsgPackConstants.*;
import static com.bantanger.im.common.enums.command.MessageCommand.MSG_P2P;

/**
 * TCP 层校验消息发送方合法性
 * @author BanTanger 半糖
 * @Date 2023/4/5 20:06
 */
@Slf4j
public class CheckLegalMsgCommand extends BaseCommandStrategy {

    @Override
    public void systemStrategy(CommandExecution commandExecution) {
        PostProcessorContainer postProcessorContainer =
                PostProcessorContainer.getInstance(CheckLegalMsgPostProcessor.class);

        PostContext<FeignDataBO> feignDataPostContext = new PostContext<>();
        FeignDataBO feignDataBO = new FeignDataBO(
                commandExecution.getCtx(), commandExecution.getMsg());

        feignDataPostContext.setBizData(feignDataBO);

        // 预留点前切点: 暂无
        boolean isContinue = postProcessorContainer.handleBefore(feignDataPostContext);
        if (!isContinue) {
            log.error("[FeignMessageService] 预留点前切点执行有失败，无法执行后续逻辑！");
            return ;
        }

        // 主流程业务逻辑执行: 调用业务层校验消息发送方的内部接口
        CheckSendMessageReq req = getCheckSendMsgReq(commandExecution.getMsg());
        FeignMessageService feignMessageService = commandExecution.getFeignMessageService();
        ResponseVO responseVO = feignMessageService.checkP2PSendMessage(req);

        feignDataPostContext.setMainProcessRes(responseVO);

        // 预留点后切点: 校验结果异步通知
        postProcessorContainer.handleAfter(feignDataPostContext);

    }

    private CheckSendMessageReq getCheckSendMsgReq(Message msg) {
        JSONObject msgPack = JSON.parseObject(JSONObject.toJSONString(msg.getMessagePack()));

        return CheckSendMessageReq.builder()
                .command(msg.getMessageHeader().getCommand())
                .appId(msg.getMessageHeader().getAppId())
                .fromId(msgPack.getString(FROM_ID))
                .toId(msgPack.getString(msg.getMessageHeader().getCommand()
                                .equals(MSG_P2P.getCommand()) ? TO_ID : GROUP_ID))
                .build();
    }

}