package com.bantanger.im.service.strategy.command.message.extensionpost;

import com.bantanger.im.service.support.postprocessor.BasePostProcessor;

/**
 * <p> 该接口空实现，仅作为 PostProcessor 具体分支供子类继承 </p>
 * <p> 鉴权消息发送是否合法 </p>
 * 合法消息定义：<br>
 * 1. 消息通过敏感词过滤（未做 TODO）
 * 2. 消息发送和接收方非风控、为强好友关系
 * @author BanTanger 半糖
 * @Date 2023/12/16 23:44
 */
public interface CheckLegalMsgPostProcessor extends BasePostProcessor<FeignDataBO> {

}
