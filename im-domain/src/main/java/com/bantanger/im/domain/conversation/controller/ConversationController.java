package com.bantanger.im.domain.conversation.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.conversation.model.DeleteConversationReq;
import com.bantanger.im.domain.conversation.model.UpdateConversationReq;
import com.bantanger.im.domain.conversation.service.ConversationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/8 17:23
 */
@RestController
@RequestMapping("v1/conversation")
public class ConversationController {

    @Resource
    ConversationService conversationServiceImpl;

    @RequestMapping("/deleteConversation")
    public ResponseVO deleteConversation(@RequestBody @Validated DeleteConversationReq req,
                                         Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationServiceImpl.deleteConversation(req);
    }

    @RequestMapping("/updateConversation")
    public ResponseVO updateConversation(@RequestBody @Validated UpdateConversationReq req,
                                         Integer appId, String identifier) {
        req.setAppId(appId);
//        req.setOperater(identifier);
        return conversationServiceImpl.updateConversation(req);
    }

}
