package com.bantanger.im.domain.friendship.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.ApprovalFriendRequestReq;
import com.bantanger.im.domain.friendship.model.req.friend.GetFriendShipRequestReq;
import com.bantanger.im.domain.friendship.model.req.friend.ReadFriendShipRequestReq;
import com.bantanger.im.domain.friendship.service.ImFriendShipRequestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping("v1/friendshipRequest")
public class ImFriendShipRequestController {

    @Resource
    ImFriendShipRequestService imFriendShipRequestService;

    @RequestMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody @Validated ApprovalFriendRequestReq req,
                                           Integer appId, String identifier){
        req.setAppId(appId);
        req.setOperater(identifier);
        return imFriendShipRequestService.approvalFriendRequest(req);
    }
    @RequestMapping("/getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipRequestService.getFriendRequest(req.getFromId(),req.getAppId());
    }

    @RequestMapping("/readFriendShipRequestReq")
    public ResponseVO readFriendShipRequestReq(@RequestBody @Validated ReadFriendShipRequestReq req, Integer appId){
        req.setAppId(appId);
        return imFriendShipRequestService.readFriendShipRequestReq(req);
    }


}
