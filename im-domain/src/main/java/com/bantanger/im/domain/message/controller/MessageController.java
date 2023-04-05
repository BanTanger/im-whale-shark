package com.bantanger.im.domain.message.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.message.model.req.SendMessageReq;
import com.bantanger.im.domain.message.service.P2PMessageService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 后台发送消息控制层
 * @author BanTanger 半糖
 * @Date 2023/4/5 17:06
 */
@RestController
@RequestMapping("v1/message")
public class MessageController {

    @Resource
    P2PMessageService p2PMessageService;

    @RequestMapping("/send")
    public ResponseVO send(@RequestBody @Validated SendMessageReq req, Integer appId)  {
        req.setAppId(appId);
        return ResponseVO.successResponse(p2PMessageService.send(req));
    }

}
