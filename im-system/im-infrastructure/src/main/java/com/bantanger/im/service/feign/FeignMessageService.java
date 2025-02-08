package com.bantanger.im.service.feign;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.message.CheckSendMessageReq;
import feign.Headers;
import feign.RequestLine;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/5 19:16
 */
public interface FeignMessageService {

    /**
     * RPC 调度业务层的接口，接口职责为检查 [P2P] 发送方是否有权限
     * @param o
     * @return
     */
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /message/p2pCheckSend")
    ResponseVO checkP2PSendMessage(CheckSendMessageReq o);

    /**
     * RPC 调度业务层的接口，接口职责为检查 [GROUP] 发送方是否有权限
     * @param o
     * @return
     */
    @Headers({"Content-Type: application/json","Accept: application/json"})
    @RequestLine("POST /message/groupCheckSend")
    ResponseVO checkGroupSendMessage(CheckSendMessageReq o);

}
