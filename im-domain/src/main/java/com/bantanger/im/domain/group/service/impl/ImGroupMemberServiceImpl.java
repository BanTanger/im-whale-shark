package com.bantanger.im.domain.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.comstant.Constants;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.domain.group.model.req.callback.AddMemberAfterCallback;
import com.bantanger.im.domain.group.model.resp.AddMemberResp;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import com.bantanger.im.domain.group.service.ImGroupService;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.service.callback.CallbackService;
import com.bantanger.im.service.config.AppConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.group.GroupErrorCode;
import com.bantanger.im.common.enums.group.GroupMemberRoleEnum;
import com.bantanger.im.common.enums.group.GroupStatusEnum;
import com.bantanger.im.common.enums.group.GroupTypeEnum;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.domain.group.dao.ImGroupEntity;
import com.bantanger.im.domain.group.dao.ImGroupMemberEntity;
import com.bantanger.im.domain.group.dao.mapper.ImGroupMemberMapper;
import com.bantanger.im.domain.group.model.resp.GetRoleInGroupResp;
import com.bantanger.im.domain.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Service
@Slf4j
public class ImGroupMemberServiceImpl implements ImGroupMemberService {

    @Resource
    ImGroupMemberMapper imGroupMemberMapper;

    @Resource
    ImGroupService groupService;

    @Resource
    ImGroupMemberService groupMemberService;

    @Resource
    ImUserService imUserService;

    @Resource
    CallbackService callbackService;

    @Resource
    AppConfig appConfig;

    @Override
    public ResponseVO importGroupMember(ImportGroupMemberReq req) {

        List<AddMemberResp> resp = new ArrayList<>();
        // 查看是否存在目标群组
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        for (GroupMemberDto memberId : req.getMembers()) {
            ResponseVO responseVO = null;
            try {
                responseVO = groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), memberId);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
            } else {
                addMemberResp.setResult(1);
            }
            resp.add(addMemberResp);
        }

        return ResponseVO.successResponse(resp);
    }

    /**
     * 添加群成员，内部调用
     *
     * @return com.bantanger.im.common.ResponseVO
     */
    @Override
    @Transactional
    public ResponseVO addGroupMember(String groupId, Integer appId, GroupMemberDto dto) {

        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(dto.getMemberId(), appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        // 查询是否有群主
        if (dto.getRole() != null && GroupMemberRoleEnum.OWNER.getCode() == dto.getRole()) {
            QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
            queryOwner.eq("group_id", groupId);
            queryOwner.eq("app_id", appId);
            queryOwner.eq("role", GroupMemberRoleEnum.OWNER.getCode());
            Integer ownerNum = imGroupMemberMapper.selectCount(queryOwner);
            if (ownerNum > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }

        QueryWrapper<ImGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        query.eq("app_id", appId);
        query.eq("member_id", dto.getMemberId());
        ImGroupMemberEntity memberDto = imGroupMemberMapper.selectOne(query);

        long now = System.currentTimeMillis();
        if (memberDto == null) {
            //初次加群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, memberDto);
            memberDto.setGroupId(groupId);
            memberDto.setAppId(appId);
            memberDto.setJoinTime(now);
            int insert = imGroupMemberMapper.insert(memberDto);
            if (insert == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        } else if (GroupMemberRoleEnum.LEAVE.getCode() == memberDto.getRole()) {
            //重新进群
            memberDto = new ImGroupMemberEntity();
            BeanUtils.copyProperties(dto, memberDto);
            memberDto.setJoinTime(now);
            int update = imGroupMemberMapper.update(memberDto, query);
            if (update == 1) {
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }

        return ResponseVO.errorResponse(GroupErrorCode.USER_IS_JOINED_GROUP);

    }

    @Override
    public ResponseVO removeGroupMember(String groupId, Integer appId, String memberId) {

        ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(memberId, appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        ResponseVO<GetRoleInGroupResp> roleInGroupOne = getRoleInGroupOne(groupId, memberId, appId);
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        GetRoleInGroupResp data = roleInGroupOne.getData();
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.LEAVE.getCode());
        imGroupMemberEntity.setLeaveTime(System.currentTimeMillis());
        imGroupMemberEntity.setGroupMemberId(data.getGroupMemberId());
        imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<GetRoleInGroupResp> getRoleInGroupOne(String groupId, String memberId, Integer appId) {

        GetRoleInGroupResp resp = new GetRoleInGroupResp();

        QueryWrapper<ImGroupMemberEntity> queryOwner = new QueryWrapper<>();
        queryOwner.eq("group_id", groupId);
        queryOwner.eq("app_id", appId);
        queryOwner.eq("member_id", memberId);

        ImGroupMemberEntity imGroupMemberEntity = imGroupMemberMapper.selectOne(queryOwner);
        if (imGroupMemberEntity == null || imGroupMemberEntity.getRole() == GroupMemberRoleEnum.LEAVE.getCode()) {
            return ResponseVO.errorResponse(GroupErrorCode.MEMBER_IS_NOT_JOINED_GROUP);
        }

        resp.setSpeakDate(imGroupMemberEntity.getSpeakDate());
        resp.setGroupMemberId(imGroupMemberEntity.getGroupMemberId());
        resp.setMemberId(imGroupMemberEntity.getMemberId());
        resp.setRole(imGroupMemberEntity.getRole());
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<Collection<String>> getMemberJoinedGroup(GetJoinedGroupReq req) {

        if (req.getLimit() != null) {
            Page<ImGroupMemberEntity> objectPage = new Page<>(req.getOffset(), req.getLimit());
            QueryWrapper<ImGroupMemberEntity> query = new QueryWrapper<>();
            query.eq("app_id", req.getAppId());
            query.eq("member_id", req.getMemberId());
            IPage<ImGroupMemberEntity> imGroupMemberEntityPage = imGroupMemberMapper.selectPage(objectPage, query);

            Set<String> groupId = new HashSet<>();
            List<ImGroupMemberEntity> records = imGroupMemberEntityPage.getRecords();
            records.forEach(e -> {
                groupId.add(e.getGroupId());
            });

            return ResponseVO.successResponse(groupId);
        } else {
            return ResponseVO.successResponse(imGroupMemberMapper.getJoinedGroupId(req.getAppId(), req.getMemberId()));
        }
    }

    @Override
    public ResponseVO addMember(AddGroupMemberReq req) {

        List<AddMemberResp> resp = new ArrayList<>();

        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();
        // 事件之前回调
        if(appConfig.isAddGroupMemberBeforeCallback()){
            ResponseVO responseVO = callbackService.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.GroupMemberAddBefore
                    , JSONObject.toJSONString(req));
            if(!responseVO.isOk()){
                return responseVO;
            }
            try {
                // 成员信息回调，用户可选择是否变更添加人员
                memberDtos = JSONArray.parseArray(
                        JSONObject.toJSONString(responseVO.getData()),
                        GroupMemberDto.class);
            }catch (Exception e){
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}",req.getAppId());
            }
        }

        ImGroupEntity group = groupResp.getData();

        /**
         * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
         * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
         * 群类型 1私有群（类似微信） 2公开群(类似qq）
         */
        if (!isAdmin && GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();
        for (GroupMemberDto memberId : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO = groupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), memberId);
            } catch (Exception e) {
                e.printStackTrace();
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            addMemberResp.setMemberId(memberId.getMemberId());
            if (responseVO.isOk()) {
                successId.add(memberId.getMemberId());
                addMemberResp.setResult(0);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(2);
                addMemberResp.setResultMessage(responseVO.getMsg());
            } else {
                addMemberResp.setResult(1);
                addMemberResp.setResultMessage(responseVO.getMsg());
            }
            resp.add(addMemberResp);
        }

        if(appConfig.isAddGroupMemberAfterCallback()){
            AddMemberAfterCallback dto = new AddMemberAfterCallback();
            dto.setGroupId(req.getGroupId());
            dto.setGroupType(group.getGroupType());
            dto.setMemberId(resp);
            dto.setOperater(req.getOperater());
            callbackService.afterCallback(req.getAppId()
                    ,Constants.CallbackCommand.GroupMemberAddAfter,
                    JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO removeMember(RemoveGroupMemberReq req) {

        boolean isAdmin = false;
        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        ImGroupEntity group = groupResp.getData();

        if (!isAdmin) {
            if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {

                //获取操作人的权限 是管理员 or 群主 or 群成员
                ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if (!role.isOk()) {
                    return role;
                }

                GetRoleInGroupResp data = role.getData();
                Integer roleInfo = data.getRole();

                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

                // 既不是群主也不是管理员
                if (!isOwner && !isManager) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //私有群必须是群主才能踢人
                if (!isOwner && GroupTypeEnum.PRIVATE.getCode() == group.getGroupType()) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

                // 公开群管理员和群主可踢人，但管理员只能踢普通群成员
                if (GroupTypeEnum.PUBLIC.getCode() == group.getGroupType()) {
//                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                    // 获取被踢人的权限
                    ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                    if (!roleInGroupOne.isOk()) {
                        return roleInGroupOne;
                    }
                    GetRoleInGroupResp memberRole = roleInGroupOne.getData();
                    if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                        throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                    }
                    // 是管理员并且被踢人不是群成员，无法操作
                    if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                        throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                    }
                }
            }
        }
        ResponseVO responseVO = groupMemberService.removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());

        // 事件之后回调
        if(responseVO.isOk()){
            if(appConfig.isDeleteGroupMemberAfterCallback()){
                callbackService.afterCallback(req.getAppId(),
                        Constants.CallbackCommand.GroupMemberDeleteAfter,
                        JSONObject.toJSONString(req));
            }
        }
        return responseVO;
    }

    @Override
    public ResponseVO<List<GroupMemberDto>> getGroupMember(String groupId, Integer appId) {
        List<GroupMemberDto> groupMember = imGroupMemberMapper.getGroupMember(appId, groupId);
        return ResponseVO.successResponse(groupMember);
    }

    @Override
    public List<String> getGroupMemberId(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupMemberId(appId, groupId);
    }

    @Override
    public List<GroupMemberDto> getGroupManager(String groupId, Integer appId) {
        return imGroupMemberMapper.getGroupManager(groupId, appId);
    }

    @Override
    public ResponseVO updateGroupMember(UpdateGroupMemberReq req) {

        boolean isadmin = false;

        // 获取群基本信息
        ResponseVO<ImGroupEntity> group = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        ImGroupEntity groupData = group.getData();
        if (groupData.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        // 是否是自己修改自己的资料
        boolean isMeOperate = req.getOperater().equals(req.getMemberId());

        if (!isadmin) {
            // 昵称只能自己修改 权限只能群主或管理员修改
            if (StringUtils.isBlank(req.getAlias()) && !isMeOperate) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }

            // 如果要修改权限相关的则走下面的逻辑
            if (req.getRole() != null) {
                // 私有群不能设置管理员
                if (groupData.getGroupType() == GroupTypeEnum.PRIVATE.getCode() &&
                        req.getRole() != null && (req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() ||
                        req.getRole() == GroupMemberRoleEnum.OWNER.getCode())) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
                }

                // 获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }

                // 获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }

                GetRoleInGroupResp data = operateRoleInGroupOne.getData();
                Integer roleInfo = data.getRole();
                boolean isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
                boolean isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

                // 不是管理员不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                // 管理员只有群主能够设置
                if (req.getRole() != null && req.getRole() == GroupMemberRoleEnum.MAMAGER.getCode() && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

            }
        }

        ImGroupMemberEntity update = new ImGroupMemberEntity();

        if (StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }

        //不能直接修改为群主
        if (req.getRole() != null && req.getRole() != GroupMemberRoleEnum.OWNER.getCode()) {
            update.setRole(req.getRole());
        }

        UpdateWrapper<ImGroupMemberEntity> objectUpdateWrapper = new UpdateWrapper<>();
        objectUpdateWrapper.eq("app_id", req.getAppId());
        objectUpdateWrapper.eq("member_id", req.getMemberId());
        objectUpdateWrapper.eq("group_id", req.getGroupId());
        imGroupMemberMapper.update(update, objectUpdateWrapper);

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {

        //更新旧群主
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleEnum.ORDINARY.getCode());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("app_id", appId);
        updateWrapper.eq("group_id", groupId);
        updateWrapper.eq("role", GroupMemberRoleEnum.OWNER.getCode());
        imGroupMemberMapper.update(imGroupMemberEntity, updateWrapper);

        //更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRoleEnum.OWNER.getCode());
        UpdateWrapper<ImGroupMemberEntity> ownerWrapper = new UpdateWrapper<>();
        ownerWrapper.eq("app_id", appId);
        ownerWrapper.eq("group_id", groupId);
        ownerWrapper.eq("member_id", owner);
        imGroupMemberMapper.update(newOwner, ownerWrapper);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO speak(SpeaMemberReq req) {

        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        boolean isadmin = false;
        boolean isOwner = false;
        boolean isManager = false;
        GetRoleInGroupResp memberRole = null;

        if (!isadmin) {

            //获取操作人的权限 是管理员or群主or群成员
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            isOwner = roleInfo == GroupMemberRoleEnum.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleEnum.MAMAGER.getCode();

            if (!isOwner && !isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
            //被操作人是群主只能app管理员操作
            if (memberRole.getRole() == GroupMemberRoleEnum.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            //是管理员并且被操作人不是群成员，无法操作
            if (isManager && memberRole.getRole() != GroupMemberRoleEnum.ORDINARY.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }

        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        if (memberRole == null) {
            //获取被操作的权限
            ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
            if (!roleInGroupOne.isOk()) {
                return roleInGroupOne;
            }
            memberRole = roleInGroupOne.getData();
        }

        imGroupMemberEntity.setGroupMemberId(memberRole.getGroupMemberId());
        if (req.getSpeakDate() > 0) {
            imGroupMemberEntity.setSpeakDate(System.currentTimeMillis() + req.getSpeakDate());
        } else {
            // 解除禁言
            imGroupMemberEntity.setSpeakDate(req.getSpeakDate());
        }

        int i = imGroupMemberMapper.updateById(imGroupMemberEntity);
        return ResponseVO.successResponse();
    }


}
