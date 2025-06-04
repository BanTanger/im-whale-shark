package com.bantanger.im.domain.group.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.group.AddGroupMemberPack;
import com.bantanger.im.codec.pack.group.GroupMemberSpeakPack;
import com.bantanger.im.codec.pack.group.UpdateGroupMemberPack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.enums.group.GroupErrorCode;
import com.bantanger.im.common.enums.group.GroupMemberRoleType;
import com.bantanger.im.common.enums.group.GroupStatusType;
import com.bantanger.im.common.enums.group.GroupType;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.conversation.model.CreateConversationReq;
import com.bantanger.im.domain.conversation.service.ConversationService;
import com.bantanger.im.domain.group.GroupMessageProducer;
import com.bantanger.im.domain.group.dao.ImGroupEntity;
import com.bantanger.im.domain.group.dao.ImGroupMemberEntity;
import com.bantanger.im.domain.group.dao.mapper.ImGroupMemberMapper;
import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.domain.group.model.req.callback.AddMemberAfterCallback;
import com.bantanger.im.domain.group.model.resp.AddMemberResp;
import com.bantanger.im.domain.group.model.resp.GetRoleInGroupResp;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import com.bantanger.im.domain.group.service.ImGroupService;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.domain.user.service.ImUserService;
import com.bantanger.im.infrastructure.callback.CallbackService;
import com.bantanger.im.infrastructure.config.AppConfig;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.*;

import static com.bantanger.im.common.enums.conversation.ConversationType.GROUP;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImGroupMemberServiceImpl implements ImGroupMemberService {

    private final AppConfig appConfig;
    private final ImGroupMemberMapper imGroupMemberMapper;
    private final ImGroupService groupService;
    private final ImUserService imUserService;
    private final ConversationService conversationServiceImpl;
    private final CallbackService callbackService;
    @Resource
    private GroupMessageProducer groupMessageProducer;

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
                responseVO = addGroupMember(req.getGroupId(), req.getAppId(), memberId);
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
        if (Objects.equals(GroupMemberRoleType.OWNER.getCode(), dto.getRole())) {
            Integer ownerNum = imGroupMemberMapper.selectCount(new LambdaQueryWrapper<ImGroupMemberEntity>()
                    .eq(ImGroupMemberEntity::getGroupId, groupId)
                    .eq(ImGroupMemberEntity::getAppId, appId)
                    .eq(ImGroupMemberEntity::getRole, GroupMemberRoleType.OWNER.getCode()));
            if (ownerNum > 0) {
                return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_HAVE_OWNER);
            }
        }

        LambdaQueryWrapper<ImGroupMemberEntity> queryWrapper = new LambdaQueryWrapper<ImGroupMemberEntity>()
                .eq(ImGroupMemberEntity::getGroupId, groupId)
                .eq(ImGroupMemberEntity::getAppId, appId)
                .eq(ImGroupMemberEntity::getMemberId, dto.getMemberId());
        ImGroupMemberEntity memberDto = imGroupMemberMapper.selectOne(queryWrapper);


        if (memberDto == null) {
            //初次加群
            memberDto = buildGroupMemberJoinModel(groupId, appId, dto);
            int insert = imGroupMemberMapper.insert(memberDto);
            if (insert == 1) {
                // 创建会话
                doCreateGroupConversation(dto.getMemberId(), groupId, appId);
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        } else if (GroupMemberRoleType.LEAVE.getCode().equals(memberDto.getRole())) {
            //重新进群
            memberDto = buildGroupMemberJoinModel(groupId, appId, dto);
            int update = imGroupMemberMapper.update(memberDto, queryWrapper);
            if (update == 1) {
                // 退群用户 conversation 不会删除，因此这里不用创建
//                doCreateGroupConversation(dto.getMemberId(), groupId, appId);
                return ResponseVO.successResponse();
            }
            return ResponseVO.errorResponse(GroupErrorCode.USER_JOIN_GROUP_ERROR);
        }

        return ResponseVO.errorResponse(GroupErrorCode.USER_IS_JOINED_GROUP);

    }

    private static ImGroupMemberEntity buildGroupMemberJoinModel(String groupId, Integer appId, GroupMemberDto dto) {
        ImGroupMemberEntity memberDto = new ImGroupMemberEntity();
        memberDto.setMemberId(dto.getMemberId());
        memberDto.setRole(dto.getRole());
        memberDto.setSpeakDate(dto.getSpeakDate());
        memberDto.setAlias(dto.getAlias());
        memberDto.setJoinTime(dto.getJoinTime());
        memberDto.setJoinType(dto.getJoinType());
        memberDto.setGroupId(groupId);
        memberDto.setAppId(appId);
        memberDto.setJoinTime(System.currentTimeMillis());
        return memberDto;
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
        imGroupMemberEntity.setRole(GroupMemberRoleType.LEAVE.getCode());
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
        if (imGroupMemberEntity == null || imGroupMemberEntity.getRole() == GroupMemberRoleType.LEAVE.getCode()) {
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
        boolean isAdmin = false;
        Integer appId = req.getAppId();

        ResponseVO<ImGroupEntity> groupResp = groupService.getGroup(req.getGroupId(), appId);
        if (!groupResp.isOk()) {
            return groupResp;
        }

        List<GroupMemberDto> memberDtos = req.getMembers();
        // 事件之前回调
        if (appConfig.isAddGroupMemberBeforeCallback()) {
            ResponseVO responseVO = callbackService.beforeCallback(appId,
                    Constants.CallbackCommand.GroupMemberAddBefore
                    , JSONObject.toJSONString(req));
            if (!responseVO.isOk()) {
                return responseVO;
            }
            try {
                // 成员信息回调，用户可选择是否变更添加人员
                memberDtos = JSONArray.parseArray(
                        JSONObject.toJSONString(responseVO.getData()),
                        GroupMemberDto.class);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("GroupMemberAddBefore 回调失败：{}", appId);
            }
        }

        ImGroupEntity groupBody = groupResp.getData();

        /**
         * 私有群（private）	类似普通微信群，创建后仅支持已在群内的好友邀请加群，且无需被邀请方同意或群主审批
         * 公开群（Public）	类似 QQ 群，创建后群主可以指定群管理员，需要群主或管理员审批通过才能入群
         * 群类型 1私有群（类似微信） 2公开群(类似qq）
         */
        if (!isAdmin && Objects.equals(GroupType.PUBLIC.getCode(), groupBody.getGroupType())) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
        }

        List<String> successId = new ArrayList<>();
        List<AddMemberResp> resp = new ArrayList<>();
        for (GroupMemberDto memberDto : memberDtos) {
            ResponseVO responseVO = null;
            try {
                responseVO = addGroupMember(req.getGroupId(), appId, memberDto);
            } catch (Exception e) {
                log.error(e.getMessage());
                responseVO = ResponseVO.errorResponse();
            }
            AddMemberResp addMemberResp = new AddMemberResp();
            String memberId = memberDto.getMemberId();
            addMemberResp.setMemberId(memberId);
            if (responseVO.isOk()) {
                // 记录成功加入群聊的用户信息
                successId.add(memberId);
                addMemberResp.setResult(AddMemberResp.AddGroupResultEnum.SUCCESS.getCode());

                // 为成功加入群聊的用户创建会话
                doCreateGroupConversation(memberId, groupBody.getGroupId(), appId);
            } else if (responseVO.getCode() == GroupErrorCode.USER_IS_JOINED_GROUP.getCode()) {
                addMemberResp.setResult(AddMemberResp.AddGroupResultEnum.REPEAT_JOIN.getCode());
                addMemberResp.setResultMessage(AddMemberResp.AddGroupResultEnum.REPEAT_JOIN.getMessage());
            } else {
                addMemberResp.setResult(AddMemberResp.AddGroupResultEnum.FAIL.getCode());
                addMemberResp.setResultMessage(AddMemberResp.AddGroupResultEnum.FAIL.getMessage());
            }
            resp.add(addMemberResp);
        }

        // TCP 通知
        AddGroupMemberPack addGroupMemberPack = new AddGroupMemberPack();
        addGroupMemberPack.setGroupId(req.getGroupId());
        addGroupMemberPack.setMembers(successId);
        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.ADDED_MEMBER, addGroupMemberPack
                , new ClientInfo(appId, req.getClientType(), req.getImei()));

        // 事件处理回调
        if (appConfig.isAddGroupMemberAfterCallback()) {
            AddMemberAfterCallback dto = new AddMemberAfterCallback();
            dto.setGroupId(req.getGroupId());
            dto.setGroupType(groupBody.getGroupType());
            dto.setMemberId(resp);
            dto.setOperator(req.getOperator());
            callbackService.afterCallback(appId
                    , Constants.CallbackCommand.GroupMemberAddAfter,
                    JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse(resp);
    }

    private void doCreateGroupConversation(String memberId, String groupId, Integer appId) {
        CreateConversationReq createConversationReq = new CreateConversationReq();
        createConversationReq.setConversationType(GROUP.getCode());
        createConversationReq.setFromId(memberId);
        createConversationReq.setToId(groupId);
        createConversationReq.setAppId(appId);
        conversationServiceImpl.createConversation(createConversationReq);
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
            if (Objects.equals(GroupType.PUBLIC.getCode(), group.getGroupType())) {

                //获取操作人的权限 是管理员 or 群主 or 群成员
                ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                if (!role.isOk()) {
                    return role;
                }

                GetRoleInGroupResp data = role.getData();
                Integer roleInfo = data.getRole();

                boolean isOwner = Objects.equals(roleInfo, GroupMemberRoleType.OWNER.getCode());
                boolean isManager = Objects.equals(roleInfo, GroupMemberRoleType.MANAGER.getCode());

                // 既不是群主也不是管理员
                if (!isOwner && !isManager) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                //私有群必须是群主才能踢人
                if (!isOwner && Objects.equals(GroupType.PRIVATE.getCode(), group.getGroupType())) {
                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

                // 公开群管理员和群主可踢人，但管理员只能踢普通群成员
                if (Objects.equals(GroupType.PUBLIC.getCode(), group.getGroupType())) {
//                    throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                    // 获取被踢人的权限
                    ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                    if (!roleInGroupOne.isOk()) {
                        return roleInGroupOne;
                    }
                    GetRoleInGroupResp memberRole = roleInGroupOne.getData();
                    if (Objects.equals(memberRole.getRole(), GroupMemberRoleType.OWNER.getCode())) {
                        throw new ApplicationException(GroupErrorCode.GROUP_OWNER_IS_NOT_REMOVE);
                    }
                    // 是管理员并且被踢人不是群成员，无法操作
                    if (isManager && !Objects.equals(memberRole.getRole(), GroupMemberRoleType.MEMBER.getCode())) {
                        throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                    }
                }
            }
        }
        ResponseVO responseVO = removeGroupMember(req.getGroupId(), req.getAppId(), req.getMemberId());


        if (responseVO.isOk()) {
            // TCP 通知
            RemoveGroupMemberPack removeGroupMemberPack = new RemoveGroupMemberPack();
            removeGroupMemberPack.setGroupId(req.getGroupId());
            removeGroupMemberPack.setMember(req.getMemberId());
            groupMessageProducer.producer(req.getMemberId(), GroupEventCommand.DELETED_MEMBER, removeGroupMemberPack
                    , new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

            // 事件之后回调
            if (appConfig.isDeleteGroupMemberAfterCallback()) {
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
        return imGroupMemberMapper.getGroupManager(appId, groupId);
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
        if (Objects.equals(groupData.getStatus(), GroupStatusType.DESTROY.getCode())) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        // 是否是自己修改自己的资料
        boolean isMeOperate = req.getOperator().equals(req.getMemberId());

        if (!isadmin) {
            // 昵称只能自己修改 权限只能群主或管理员修改
            if (StringUtils.isBlank(req.getAlias()) && !isMeOperate) {
                return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_ONESELF);
            }

            // 如果要修改权限相关的则走下面的逻辑
            if (req.getRole() != null) {
                // 私有群不能设置管理员
                if (Objects.equals(groupData.getGroupType(), GroupType.PRIVATE.getCode())
                    && (Objects.equals(req.getRole(), GroupMemberRoleType.MANAGER.getCode()) ||
                        Objects.equals(req.getRole(), GroupMemberRoleType.OWNER.getCode()))) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
                }

                // 获取被操作人的是否在群内
                ResponseVO<GetRoleInGroupResp> roleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getMemberId(), req.getAppId());
                if (!roleInGroupOne.isOk()) {
                    return roleInGroupOne;
                }

                // 获取操作人权限
                ResponseVO<GetRoleInGroupResp> operateRoleInGroupOne = this.getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
                if (!operateRoleInGroupOne.isOk()) {
                    return operateRoleInGroupOne;
                }

                GetRoleInGroupResp data = operateRoleInGroupOne.getData();
                boolean isOwner = Objects.equals(data.getRole(), GroupMemberRoleType.OWNER.getCode());
                boolean isManager = Objects.equals(data.getRole(), GroupMemberRoleType.MANAGER.getCode());

                // 不是管理员不能修改权限
                if (req.getRole() != null && !isOwner && !isManager) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
                }

                // 管理员只有群主能够设置
                if (Objects.equals(req.getRole(), GroupMemberRoleType.MANAGER.getCode()) && !isOwner) {
                    return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
                }

            }
        }

        ImGroupMemberEntity update = new ImGroupMemberEntity();

        if (StringUtils.isNotBlank(req.getAlias())) {
            update.setAlias(req.getAlias());
        }

        //不能直接修改为群主
        if (req.getRole() != null && req.getRole() != GroupMemberRoleType.OWNER.getCode()) {
            update.setRole(req.getRole());
        }

        UpdateWrapper<ImGroupMemberEntity> objectUpdateWrapper = new UpdateWrapper<>();
        objectUpdateWrapper.eq("app_id", req.getAppId());
        objectUpdateWrapper.eq("member_id", req.getMemberId());
        objectUpdateWrapper.eq("group_id", req.getGroupId());
        imGroupMemberMapper.update(update, objectUpdateWrapper);

        UpdateGroupMemberPack pack = new UpdateGroupMemberPack();
        pack.setGroupId(req.getGroupId());
        pack.setMemberId(req.getMemberId());
        pack.setAlias(req.getAlias());
        pack.setExtra(req.getExtra());

        groupMessageProducer.producer(req.getOperator(), GroupEventCommand.UPDATED_MEMBER, pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO transferGroupMember(String owner, String groupId, Integer appId) {

        //更新旧群主
        ImGroupMemberEntity imGroupMemberEntity = new ImGroupMemberEntity();
        imGroupMemberEntity.setRole(GroupMemberRoleType.MEMBER.getCode());
        UpdateWrapper<ImGroupMemberEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("app_id", appId);
        updateWrapper.eq("group_id", groupId);
        updateWrapper.eq("role", GroupMemberRoleType.OWNER.getCode());
        imGroupMemberMapper.update(imGroupMemberEntity, updateWrapper);

        //更新新群主
        ImGroupMemberEntity newOwner = new ImGroupMemberEntity();
        newOwner.setRole(GroupMemberRoleType.OWNER.getCode());
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
            ResponseVO<GetRoleInGroupResp> role = getRoleInGroupOne(req.getGroupId(), req.getOperator(), req.getAppId());
            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            isOwner = roleInfo == GroupMemberRoleType.OWNER.getCode();
            isManager = roleInfo == GroupMemberRoleType.MANAGER.getCode();

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
            if (memberRole.getRole() == GroupMemberRoleType.OWNER.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_APPMANAGER_ROLE);
            }

            //是管理员并且被操作人不是群成员，无法操作
            if (isManager && memberRole.getRole() != GroupMemberRoleType.MEMBER.getCode()) {
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
        if (i == 1) {
            GroupMemberSpeakPack pack = new GroupMemberSpeakPack();
            pack.setGroupId(req.getGroupId());
            pack.setMemberId(req.getMemberId());
            pack.setSpeakDate(req.getSpeakDate());

            groupMessageProducer.producer(req.getOperator(), GroupEventCommand.SPEAK_GROUP_MEMBER, pack,
                    new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO<Collection<String>> syncMemberJoinedGroup(String operater, Integer appId) {
        // 离开群聊的用户无法获取数据
        List<String> groupIds = imGroupMemberMapper.syncJoinedGroupId(appId, operater, GroupMemberRoleType.LEAVE.getCode());
        return ResponseVO.successResponse(groupIds);
    }
}
