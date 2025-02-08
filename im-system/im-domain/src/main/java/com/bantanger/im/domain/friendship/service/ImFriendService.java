package com.bantanger.im.domain.friendship.service;

import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.domain.friendship.model.req.*;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.model.RequestBase;
import com.bantanger.im.domain.friendship.model.req.friend.*;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
public interface ImFriendService {

    /**
     * 导入其他系统的好友关系
     * @param req
     * @return
     */
    ResponseVO importFriendShip(ImportFriendShipReq req);

    /**
     * 添加好友逻辑
     * @param req
     * @return
     */
    ResponseVO addFriend(AddFriendReq req);

    ResponseVO updateFriend(UpdateFriendReq req);

    ResponseVO deleteFriend(DeleteFriendReq req);

    ResponseVO deleteAllFriend(DeleteFriendReq req);

    /**
     * 查询所有好友关系
     * @param req fromId
     * @return
     */
    ResponseVO getAllFriendShip(GetAllFriendShipReq req);

    /**
     * 查询指定好友关系 [是否落库持久化]
     * @param req fromId、toId
     * @return
     */
    ResponseVO getRelation(GetRelationReq req);

    ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId);

    ResponseVO checkFriendship(CheckFriendShipReq req);

    ResponseVO addBlack(AddFriendShipBlackReq req);

    ResponseVO deleteBlack(DeleteBlackReq req);

    ResponseVO checkBlck(CheckFriendShipReq req);

    /**
     * 增量拉取好友关系
     * @param req
     * @return
     */
    ResponseVO syncFriendShipList(SyncReq req);
}
