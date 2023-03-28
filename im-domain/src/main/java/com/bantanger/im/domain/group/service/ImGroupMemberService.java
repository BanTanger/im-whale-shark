package com.bantanger.im.domain.group.service;

import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.group.model.resp.GetRoleInGroupResp;

import java.util.Collection;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
public interface ImGroupMemberService {

    /**
     * 批量导入群组用户
     * @param req
     * @return
     */
    ResponseVO importGroupMember(ImportGroupMemberReq req);

    /**
     * 【只有私有群可以调用本接口】
     * 添加群成员，拉人入群的逻辑，直接进入群聊。如果是后台管理员，则直接拉入群，
     * 否则只有私有群可以调用本接口，并且群成员也可以拉人入群.
     * @param req
     * @return
     */
    ResponseVO addMember(AddGroupMemberReq req);

    /**
     * 【外部调用】 删除群成员
     * @param req
     * @return
     */
    ResponseVO removeMember(RemoveGroupMemberReq req);

    /**
     * 【公有群调用本接口】
     * 添加群成员，拉人入群的逻辑，直接进入群聊。如果是后台管理员，则直接拉入群，
     * 否则只有私有群可以调用本接口，并且群成员也可以拉人入群.
     * @param groupId
     * @param appId
     * @param dto 请求的用户信息
     * @return 群组成功添加该用户
     */
    ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto);

    /**
     * 【内部调用】 删除群成员
     * @param groupId
     * @param appId
     * @param memberId
     * @return
     */
    ResponseVO removeGroupMember(String groupId, Integer appId, String memberId);

    /**
     * 获取该用户在群里的角色
     * @param groupId
     * @param memberId
     * @param appId
     * @return
     */
    ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId);

    /**
     * 获取用户所加入的所有群组 id
     * @param req
     * @return
     */
    ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req);

    ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId);

    List<String> getGroupMemberId(String groupId, Integer appId);

    List<GroupMemberDto> getGroupManager(String groupId, Integer appId);

    /**
     * 修改群成员信息
     * @param req
     * @return
     */
    ResponseVO updateGroupMember(UpdateGroupMemberReq req);

    /**
     * 群主身份转让
     * @param owner
     * @param groupId
     * @param appId
     * @return
     */
    ResponseVO transferGroupMember(String owner, String groupId, Integer appId);

    /**
     * 禁言功能
     * @param req
     * @return
     */
    ResponseVO speak(SpeaMemberReq req);

}
