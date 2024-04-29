package com.bantanger.im.domain.friendship.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.ApprovalFriendRequestReq;
import com.bantanger.im.domain.friendship.model.req.friend.GetFriendShipRequestReq;
import com.bantanger.im.domain.friendship.model.req.friend.ReadFriendShipRequestReq;
import com.bantanger.im.domain.friendship.service.ImFriendShipRequestService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping("v1/friendshipRequest")
public class ImFriendShipRequestController {

    @Resource
    ImFriendShipRequestService imFriendShipRequestService;

    /**
     * 好友申请审批
     * <pre>
     * curl -X POST \
     *   http://localhost:18000/v1/friendshipRequest/approveFriendRequest \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "id": 2,
     *     "status": 1,
     *     "appId": 10001,
     *     "operater": "bantanger",
     *     "clientType": 2,
     *     "imei": "200"
     *   }'
     * </pre>
     * @param req
     * @return
     */
    @PostMapping("/approveFriendRequest")
    public ResponseVO approveFriendRequest(@RequestBody @Validated ApprovalFriendRequestReq req){
        return imFriendShipRequestService.approvalFriendRequest(req);
    }
    @PostMapping("/getFriendRequest")
    public ResponseVO getFriendRequest(@RequestBody @Validated GetFriendShipRequestReq req){
        return imFriendShipRequestService.getFriendRequest(req.getFromId(),req.getAppId());
    }

    @PostMapping("/readFriendShipRequestReq")
    public ResponseVO readFriendShipRequestReq(@RequestBody @Validated ReadFriendShipRequestReq req){
        return imFriendShipRequestService.readFriendShipRequestReq(req);
    }


}
