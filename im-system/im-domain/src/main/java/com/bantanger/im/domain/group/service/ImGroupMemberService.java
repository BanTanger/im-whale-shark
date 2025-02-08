package com.bantanger.im.domain.group.service;

import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.group.model.resp.GetRoleInGroupResp;

import java.util.Collection;
import java.util.List;

/**
 * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
 * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
 * 群类型 1私有群（类似微信） 2公开群(类似qq）
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

    /**
     * 增量拉取用户被拉入群聊通知最大条目数
     * [边界情况：离开群聊但状态未更新的用户不能拉取数据]
     * @param operater
     * @param appId
     * @return
     */
    ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId);
}
