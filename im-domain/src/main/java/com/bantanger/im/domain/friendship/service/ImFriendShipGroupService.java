package com.bantanger.im.domain.friendship.service;

import com.bantanger.im.domain.friendship.dao.ImFriendShipGroupEntity;
import com.bantanger.im.domain.friendship.model.req.group.AddFriendShipGroupReq;
import com.bantanger.im.domain.friendship.model.req.group.DeleteFriendShipGroupReq;
import com.bantanger.im.common.ResponseVO;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
public interface ImFriendShipGroupService {

    ResponseVO addGroup(AddFriendShipGroupReq req);

    ResponseVO deleteGroup(DeleteFriendShipGroupReq req);

    ResponseVO<ImFriendShipGroupEntity> getGroup(String fromId, String groupName, Integer appId);

}
