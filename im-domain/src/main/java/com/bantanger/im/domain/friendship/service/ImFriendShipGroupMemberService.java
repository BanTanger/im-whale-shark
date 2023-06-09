package com.bantanger.im.domain.friendship.service;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.group.member.AddFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.model.req.group.member.DeleteFriendShipGroupMemberReq;

/**
 * @author bantanger 半糖
 */
public interface ImFriendShipGroupMemberService {

    ResponseVO addGroupMember(AddFriendShipGroupMemberReq req);

    ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req);

    int doAddGroupMember(Long groupId, String toId);

    int clearGroupMember(Long groupId);
}
