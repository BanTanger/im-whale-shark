package com.bantanger.im.domain.group.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.group.CreateGroupPack;
import com.bantanger.im.codec.pack.group.DestroyGroupPack;
import com.bantanger.im.codec.pack.group.UpdateGroupInfoPack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.GroupEventCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.SyncResp;
import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.domain.group.model.req.callback.DestroyGroupCallbackDto;
import com.bantanger.im.domain.group.model.resp.GetGroupResp;
import com.bantanger.im.domain.group.service.ImGroupService;
import com.bantanger.im.domain.group.GroupMessageProducer;
import com.bantanger.im.domain.message.seq.RedisSequence;
import com.bantanger.im.service.callback.CallbackService;
import com.bantanger.im.service.config.AppConfig;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.group.GroupErrorCode;
import com.bantanger.im.common.enums.group.GroupMemberRoleEnum;
import com.bantanger.im.common.enums.group.GroupStatusEnum;
import com.bantanger.im.common.enums.group.GroupTypeEnum;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.domain.group.dao.ImGroupEntity;
import com.bantanger.im.domain.group.dao.mapper.ImGroupMapper;
import com.bantanger.im.domain.group.model.resp.GetJoinedGroupResp;
import com.bantanger.im.domain.group.model.resp.GetRoleInGroupResp;
import com.bantanger.im.domain.group.service.ImGroupMemberService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Service
public class ImGroupServiceImpl implements ImGroupService {

    @Resource
    ImGroupMapper imGroupMapper;

    @Resource
    ImGroupMemberService imGroupMemberService;

    @Resource
    CallbackService callbackServiceImpl;

    @Resource
    AppConfig appConfig;

    @Resource
    GroupMessageProducer groupMessageProducer;

    @Resource
    RedisSequence redisSequence;

    @Override
    public ResponseVO importGroup(ImportGroupReq req) {

        //1.判断群id是否存在
        QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();

        if (StringUtils.isEmpty(req.getGroupId())) {
            // 如果请求没有群组 id，使用 UUID 策略自动生成一个唯一群组 ID
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            query.eq("group_id", req.getGroupId());
            query.eq("app_id", req.getAppId());
            Integer integer = imGroupMapper.selectCount(query);
            if (integer > 0) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }

        ImGroupEntity imGroupEntity = new ImGroupEntity();

        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        if (req.getCreateTime() == null) {
            imGroupEntity.setCreateTime(System.currentTimeMillis());
        }

        imGroupEntity.setGroupId(req.getGroupId());
        imGroupEntity.setAppId(req.getAppId());
        imGroupEntity.setOwnerId(req.getOwnerId());
        imGroupEntity.setGroupType(req.getGroupType());
        imGroupEntity.setGroupName(req.getGroupName());
        imGroupEntity.setMute(req.getMute());
        imGroupEntity.setApplyJoinType(req.getApplyJoinType());
        imGroupEntity.setIntroduction(req.getIntroduction());
        imGroupEntity.setNotification(req.getNotification());
        imGroupEntity.setPhoto(req.getPhoto());
        imGroupEntity.setMaxMemberCount(req.getMaxMemberCount());
        imGroupEntity.setExtra(req.getExtra());
        imGroupEntity.setStatus(GroupStatusEnum.NORMAL.getCode());

        int insert = imGroupMapper.insert(imGroupEntity);

        if (insert != 1) {
            throw new ApplicationException(GroupErrorCode.IMPORT_GROUP_ERROR);
        }

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO createGroup(CreateGroupReq req) {

        boolean isAdmin = false;

        if (!isAdmin) {
            req.setOwnerId(req.getOperater());
        }

        // 1.判断群 id 是否存在
        QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();

        if (StringUtils.isEmpty(req.getGroupId())) {
            req.setGroupId(UUID.randomUUID().toString().replace("-", ""));
        } else {
            query.eq("group_id", req.getGroupId());
            query.eq("app_id", req.getAppId());
            Integer integer = imGroupMapper.selectCount(query);
            if (integer > 0) {
                throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
            }
        }

        // 公开群需要指定群主
        if (req.getGroupType() == GroupTypeEnum.PUBLIC.getCode() && StringUtils.isBlank(req.getOwnerId())) {
            throw new ApplicationException(GroupErrorCode.PUBLIC_GROUP_MUST_HAVE_OWNER);
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GroupSeq);

        ImGroupEntity imGroupEntity = new ImGroupEntity();
        imGroupEntity.setSequence(seq);
        imGroupEntity.setGroupId(req.getGroupId());
        imGroupEntity.setAppId(req.getAppId());
        imGroupEntity.setOwnerId(req.getOwnerId());
        imGroupEntity.setGroupType(req.getGroupType());
        imGroupEntity.setGroupName(req.getGroupName());
        imGroupEntity.setMute(req.getMute());
        imGroupEntity.setApplyJoinType(req.getApplyJoinType());
        imGroupEntity.setIntroduction(req.getIntroduction());
        imGroupEntity.setNotification(req.getNotification());
        imGroupEntity.setPhoto(req.getPhoto());
        imGroupEntity.setMaxMemberCount(req.getMaxMemberCount());
        imGroupEntity.setExtra(req.getExtra());
        imGroupEntity.setCreateTime(System.currentTimeMillis());
        imGroupEntity.setStatus(GroupStatusEnum.NORMAL.getCode());

        int insert = imGroupMapper.insert(imGroupEntity);

        // 群主插入 GroupMember 表，并将其设置成 owner 权限
        GroupMemberDto groupMemberDto = new GroupMemberDto();
        groupMemberDto.setMemberId(req.getOwnerId());
        groupMemberDto.setRole(GroupMemberRoleEnum.OWNER.getCode());
        groupMemberDto.setJoinTime(System.currentTimeMillis());
        imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), groupMemberDto);

        //插入群成员
        for (GroupMemberDto dto : req.getMember()) {
            imGroupMemberService.addGroupMember(req.getGroupId(), req.getAppId(), dto);
        }

        // 发送 TCP 通知
        CreateGroupPack createGroupPack = new CreateGroupPack();
        createGroupPack.setGroupId(imGroupEntity.getGroupId());
        createGroupPack.setAppId(imGroupEntity.getAppId());
        createGroupPack.setOwnerId(imGroupEntity.getOwnerId());
        createGroupPack.setGroupType(imGroupEntity.getGroupType());
        createGroupPack.setGroupName(imGroupEntity.getGroupName());
        createGroupPack.setMute(imGroupEntity.getMute());
        createGroupPack.setApplyJoinType(imGroupEntity.getApplyJoinType());
        createGroupPack.setIntroduction(imGroupEntity.getIntroduction());
        createGroupPack.setNotification(imGroupEntity.getNotification());
        createGroupPack.setPhoto(imGroupEntity.getPhoto());
        createGroupPack.setStatus(imGroupEntity.getStatus());
        createGroupPack.setSequence(imGroupEntity.getSequence());
        createGroupPack.setCreateTime(imGroupEntity.getCreateTime());
        createGroupPack.setExtra(imGroupEntity.getExtra());

        groupMessageProducer.producer(req.getOperater(), GroupEventCommand.CREATED_GROUP, createGroupPack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        // 之后回调
        if (appConfig.isCreateGroupAfterCallback()) {
            callbackServiceImpl.afterCallback(req.getAppId(),
                    Constants.CallbackCommand.CreateGroupAfter,
                    JSONObject.toJSONString(imGroupEntity));
        }

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO updateBaseGroupInfo(UpdateGroupReq req) {

        //1.判断群id是否存在
        QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();
        query.eq("group_id", req.getGroupId());
        query.eq("app_id", req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(query);
        if (imGroupEntity == null) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_EXIST);
        }

        if (imGroupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        boolean isAdmin = false;

        if (!isAdmin) {
            //不是后台调用需要检查权限
            ResponseVO<GetRoleInGroupResp> role = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());

            if (!role.isOk()) {
                // 用户不在群内
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            boolean isManager = Objects.equals(roleInfo, GroupMemberRoleEnum.MANAGER.getCode()) || Objects.equals(roleInfo, GroupMemberRoleEnum.OWNER.getCode());

            //公开群只能群主修改资料
            if (!isManager && GroupTypeEnum.PUBLIC.getCode() == imGroupEntity.getGroupType()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }

        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GroupSeq);

        ImGroupEntity update = new ImGroupEntity();
        update.setSequence(seq);
        update.setGroupId(req.getGroupId());
        update.setAppId(req.getAppId());
        update.setGroupName(req.getGroupName());
        update.setMute(req.getMute());
        update.setApplyJoinType(req.getApplyJoinType());
        update.setIntroduction(req.getIntroduction());
        update.setNotification(req.getNotification());
        update.setPhoto(req.getPhoto());
        update.setMaxMemberCount(req.getMaxMemberCount());
        update.setExtra(req.getExtra());

        update.setUpdateTime(System.currentTimeMillis());
        int row = imGroupMapper.update(update, query);
        if (row != 1) {
            throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
        }

        // 发送 TCP 通知
        UpdateGroupInfoPack pack = new UpdateGroupInfoPack();
        pack.setSequence(seq);
        pack.setGroupId(req.getGroupId());
        pack.setGroupName(req.getGroupName());
        pack.setMute(req.getMute());
        pack.setIntroduction(req.getIntroduction());
        pack.setNotification(req.getNotification());
        pack.setPhoto(req.getPhoto());

        groupMessageProducer.producer(req.getOperater(), GroupEventCommand.UPDATED_GROUP,
                pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));


        // 之后回调
        if (appConfig.isModifyGroupAfterCallback()) {
            callbackServiceImpl.afterCallback(req.getAppId(),
                    Constants.CallbackCommand.UpdateGroupAfter,
                    // 将修改之后的群聊信息查询给服务器 TCP 服务层
                    JSONObject.toJSONString(imGroupMapper.selectOne(query)));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getJoinedGroup(GetJoinedGroupReq req) {

        // 1. 获取用户加入所有群 ID
        ResponseVO<Collection<String>> memberJoinedGroup = imGroupMemberService.getMemberJoinedGroup(req);
        if (memberJoinedGroup.isOk()) {

            GetJoinedGroupResp resp = new GetJoinedGroupResp();

            if (CollectionUtils.isEmpty(memberJoinedGroup.getData())) {
                resp.setTotalCount(0);
                resp.setGroupList(new ArrayList<>());
                return ResponseVO.successResponse(resp);
            }

            QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();
            query.eq("app_id", req.getAppId());
            query.in("group_id", memberJoinedGroup.getData());

            if (CollectionUtils.isNotEmpty(req.getGroupType())) {
                query.in("group_type", req.getGroupType());
            }

            List<ImGroupEntity> groupList = imGroupMapper.selectList(query);
            resp.setGroupList(groupList);
            if (req.getLimit() == null) {
                resp.setTotalCount(groupList.size());
            } else {
                resp.setTotalCount(imGroupMapper.selectCount(query));
            }
            return ResponseVO.successResponse(resp);
        } else {
            return memberJoinedGroup;
        }
    }

    @Override
    @Transactional
    public ResponseVO destroyGroup(DestroyGroupReq req) {

        boolean isAdmin = false;

        QueryWrapper<ImGroupEntity> objectQueryWrapper = new QueryWrapper<>();
        objectQueryWrapper.eq("group_id", req.getGroupId());
        objectQueryWrapper.eq("app_id", req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(objectQueryWrapper);
        if (imGroupEntity == null) {
            throw new ApplicationException(GroupErrorCode.PRIVATE_GROUP_CAN_NOT_DESTORY);
        }

        if (imGroupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        if (!isAdmin) {
            if (imGroupEntity.getGroupType() == GroupTypeEnum.PUBLIC.getCode()) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }

            if (imGroupEntity.getGroupType() == GroupTypeEnum.PUBLIC.getCode() &&
                    !imGroupEntity.getOwnerId().equals(req.getOperater())) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
            }
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GroupSeq);

        ImGroupEntity update = new ImGroupEntity();
        update.setSequence(seq);
        update.setStatus(GroupStatusEnum.DESTROY.getCode());
        int update1 = imGroupMapper.update(update, objectQueryWrapper);
        if (update1 != 1) {
            throw new ApplicationException(GroupErrorCode.UPDATE_GROUP_BASE_INFO_ERROR);
        }

        // 发送 TCP 通知
        DestroyGroupPack pack = new DestroyGroupPack();
        pack.setSequence(seq);
        pack.setGroupId(req.getGroupId());
        groupMessageProducer.producer(req.getOperater(),
                GroupEventCommand.DESTROY_GROUP, pack,
                new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        // 事件处理回调
        if (appConfig.isModifyGroupAfterCallback()) {
            DestroyGroupCallbackDto dto = new DestroyGroupCallbackDto();
            dto.setGroupId(req.getGroupId());
            callbackServiceImpl.afterCallback(req.getAppId()
                    , Constants.CallbackCommand.DestoryGroupAfter,
                    JSONObject.toJSONString(dto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO transferGroup(TransferGroupReq req) {

        ResponseVO<GetRoleInGroupResp> roleInGroupOne = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());
        if (!roleInGroupOne.isOk()) {
            return roleInGroupOne;
        }

        if (!Objects.equals(roleInGroupOne.getData().getRole(), GroupMemberRoleEnum.OWNER.getCode())) {
            return ResponseVO.errorResponse(GroupErrorCode.THIS_OPERATE_NEED_OWNER_ROLE);
        }

        ResponseVO<GetRoleInGroupResp> newOwnerRole = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOwnerId(), req.getAppId());
        if (!newOwnerRole.isOk()) {
            return newOwnerRole;
        }

        QueryWrapper<ImGroupEntity> objectQueryWrapper = new QueryWrapper<>();
        objectQueryWrapper.eq("group_id", req.getGroupId());
        objectQueryWrapper.eq("app_id", req.getAppId());
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(objectQueryWrapper);
        if (imGroupEntity.getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.GroupSeq);

        ImGroupEntity updateGroup = new ImGroupEntity();
        updateGroup.setSequence(seq);
        updateGroup.setOwnerId(req.getOwnerId());
        UpdateWrapper<ImGroupEntity> updateGroupWrapper = new UpdateWrapper<>();
        updateGroupWrapper.eq("app_id", req.getAppId());
        updateGroupWrapper.eq("group_id", req.getGroupId());
        imGroupMapper.update(updateGroup, updateGroupWrapper);
        imGroupMemberService.transferGroupMember(req.getOwnerId(), req.getGroupId(), req.getAppId());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroup(String groupId, Integer appId) {

        QueryWrapper<ImGroupEntity> query = new QueryWrapper<>();
        query.eq("app_id", appId);
        query.eq("group_id", groupId);
        ImGroupEntity imGroupEntity = imGroupMapper.selectOne(query);

        if (imGroupEntity == null) {
            return ResponseVO.errorResponse(GroupErrorCode.GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(imGroupEntity);
    }

    @Override
    public ResponseVO getGroup(GetGroupReq req) {

        ResponseVO group = this.getGroup(req.getGroupId(), req.getAppId());

        if (!group.isOk()) {
            return group;
        }

        GetGroupResp getGroupResp = new GetGroupResp();
        BeanUtils.copyProperties(group.getData(), getGroupResp);
        try {
            ResponseVO<List<GroupMemberDto>> groupMember = imGroupMemberService.getGroupMember(req.getGroupId(), req.getAppId());
            if (groupMember.isOk()) {
                getGroupResp.setMemberList(groupMember.getData());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResponseVO.successResponse(getGroupResp);
    }

    @Override
    public ResponseVO muteGroup(MuteGroupReq req) {

        ResponseVO<ImGroupEntity> groupResp = getGroup(req.getGroupId(), req.getAppId());
        if (!groupResp.isOk()) {
            return groupResp;
        }

        if (groupResp.getData().getStatus() == GroupStatusEnum.DESTROY.getCode()) {
            throw new ApplicationException(GroupErrorCode.GROUP_IS_DESTROY);
        }

        boolean isadmin = false;

        if (!isadmin) {
            //不是后台调用需要检查权限
            ResponseVO<GetRoleInGroupResp> role = imGroupMemberService.getRoleInGroupOne(req.getGroupId(), req.getOperater(), req.getAppId());

            if (!role.isOk()) {
                return role;
            }

            GetRoleInGroupResp data = role.getData();
            Integer roleInfo = data.getRole();

            boolean isManager = Objects.equals(roleInfo, GroupMemberRoleEnum.MANAGER.getCode()) || Objects.equals(roleInfo, GroupMemberRoleEnum.OWNER.getCode());

            //公开群只能群主修改资料
            if (!isManager) {
                throw new ApplicationException(GroupErrorCode.THIS_OPERATE_NEED_MANAGER_ROLE);
            }
        }

        ImGroupEntity update = new ImGroupEntity();
        update.setMute(req.getMute());

        UpdateWrapper<ImGroupEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("group_id", req.getGroupId());
        wrapper.eq("app_id", req.getAppId());
        imGroupMapper.update(update, wrapper);

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO syncJoinedGroupList(SyncReq req) {
        if (req.getMaxLimit() > appConfig.getJoinGroupMaxCount()) {
            // 前端传输限制，保证一次增量拉取数据量不超过配置文件的值
            req.setMaxLimit(appConfig.getJoinGroupMaxCount());
        }

        SyncResp<ImGroupEntity> resp = new SyncResp<>();

        ResponseVO<Collection<String>> memberJoinedGroup = imGroupMemberService.syncMemberJoinedGroup(req.getOperater(), req.getAppId());
        if (memberJoinedGroup.isOk()) {

            Collection<String> data = memberJoinedGroup.getData();
            QueryWrapper<ImGroupEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("app_id", req.getAppId());
            queryWrapper.in("group_id", data);
            queryWrapper.gt("sequence", req.getLastSequence());
            queryWrapper.last(" limit " + req.getMaxLimit());
            queryWrapper.orderByAsc("sequence");

            List<ImGroupEntity> list = imGroupMapper.selectList(queryWrapper);

            if (!CollectionUtils.isEmpty(list)) {
                ImGroupEntity maxSeqEntity = list.get(list.size() - 1);
                resp.setDataList(list);
                //设置最大seq
                Long maxSeq = imGroupMapper.getJoinGroupMaxSeq(data, req.getAppId());
                resp.setMaxSequence(maxSeq);
                //设置是否拉取完毕
                resp.setCompleted(maxSeqEntity.getSequence() >= maxSeq);
                return ResponseVO.successResponse(resp);
            }

        }
        resp.setCompleted(true);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public Long getUserGroupMaxSeq(String userId, Integer appId) {
        ResponseVO<Collection<String>> memberJoinedGroup =
                imGroupMemberService.syncMemberJoinedGroup(userId, appId);
        if (!memberJoinedGroup.isOk()) {
            throw new ApplicationException(500, "");
        }
        Long maxSeq = imGroupMapper.getJoinGroupMaxSeq(
                memberJoinedGroup.getData(), appId);
        return maxSeq;
    }
}
