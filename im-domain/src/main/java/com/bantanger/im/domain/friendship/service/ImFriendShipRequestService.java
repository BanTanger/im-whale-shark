package com.bantanger.im.domain.friendship.service;

import com.bantanger.im.domain.friendship.model.req.ApproverFriendRequestReq;
import com.bantanger.im.domain.friendship.model.req.ReadFriendShipRequestReq;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.FriendDto;


/**
 * @author bantanger 半糖
 */
public interface ImFriendShipRequestService {

    /**
     * 好友申请
     * @param fromId
     * @param dto
     * @param appId
     * @return
     */
    ResponseVO addFienshipRequest(String fromId, FriendDto dto, Integer appId);

    /**
     * 好友审批
     * @param req
     * @return
     */
    ResponseVO approverFriendRequest(ApproverFriendRequestReq req);

    /**
     * 好友申请读取情况【已读、未读】
     * @param req
     * @return
     */
    ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req);

    /**
     * 获取好友申请
     * @param fromId
     * @param appId
     * @return
     */
    ResponseVO getFriendRequest(String fromId, Integer appId);
}
