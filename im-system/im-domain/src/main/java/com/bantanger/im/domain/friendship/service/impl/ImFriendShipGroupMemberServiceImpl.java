package com.bantanger.im.domain.friendship.service.impl;

import com.bantanger.im.codec.pack.friendship.AddFriendGroupMemberPack;
import com.bantanger.im.codec.pack.friendship.DeleteFriendGroupMemberPack;
import com.bantanger.im.common.enums.command.FriendshipEventCommand;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.friendship.dao.ImFriendShipGroupEntity;
import com.bantanger.im.domain.friendship.dao.ImFriendShipGroupMemberEntity;
import com.bantanger.im.domain.friendship.dao.mapper.ImFriendShipGroupMemberMapper;
import com.bantanger.im.domain.friendship.model.resp.AddGroupMemberResp;
import com.bantanger.im.domain.friendship.service.ImFriendShipGroupMemberService;
import com.bantanger.im.domain.messageddd.domainservice.sendmsg.MessageProducer;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.group.member.AddFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.model.req.group.member.DeleteFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.service.ImFriendShipGroupService;
import com.bantanger.im.domain.user.service.ImUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@Service
public class ImFriendShipGroupMemberServiceImpl
        implements ImFriendShipGroupMemberService {

    @Resource
    ImFriendShipGroupMemberMapper imFriendShipGroupMemberMapper;

    @Resource
    ImFriendShipGroupService imFriendShipGroupService;

    @Resource
    ImUserService imUserService;

    @Resource
    ImFriendShipGroupMemberService thisService;

    @Resource
    MessageProducer messageProducer;

    @Override
    @Transactional
    public ResponseVO addGroupMember(AddFriendShipGroupMemberReq req) {

        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService
                .getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if (singleUserInfo.isOk()) {
                try {
                    int i = thisService.doAddGroupMember(group.getData().getGroupId(), toId);
                    if (i == 1) {
                        successId.add(toId);
                    }
                } catch (Exception e) {
                    errorId.add(toId);
                    e.printStackTrace();
                }
            }
        }
        AddGroupMemberResp resp = new AddGroupMemberResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);

        Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());

        // 发送 TCP 通知
        AddFriendGroupMemberPack pack = new AddFriendGroupMemberPack();
        pack.setFromId(req.getFromId());
        pack.setGroupName(req.getGroupName());
        pack.setToIds(successId);
        pack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_ADD,
                pack, new ClientInfo(req.getAppId(), req.getClientType(), req.getImei()));

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO delGroupMember(DeleteFriendShipGroupMemberReq req) {
        ResponseVO<ImFriendShipGroupEntity> group = imFriendShipGroupService
                .getGroup(req.getFromId(), req.getGroupName(), req.getAppId());
        if (!group.isOk()) {
            return group;
        }

        List<String> successId = new ArrayList();
        for (String toId : req.getToIds()) {
            ResponseVO<ImUserDataEntity> singleUserInfo = imUserService.getSingleUserInfo(toId, req.getAppId());
            if (singleUserInfo.isOk()) {
                int i = deleteGroupMember(group.getData().getGroupId(), toId);
                if (i == 1) {
                    successId.add(toId);
                }
            }
        }

        Long seq = imFriendShipGroupService.updateSeq(req.getFromId(), req.getGroupName(), req.getAppId());

        // 发送 TCP 通知
        DeleteFriendGroupMemberPack pack = new DeleteFriendGroupMemberPack();
        pack.setFromId(req.getFromId());
        pack.setGroupName(req.getGroupName());
        pack.setToIds(successId);
        pack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_MEMBER_DELETE,
                pack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        return ResponseVO.successResponse(successId);
    }

    @Override
    public int doAddGroupMember(Long groupId, String toId) {
        ImFriendShipGroupMemberEntity imFriendShipGroupMemberEntity = new ImFriendShipGroupMemberEntity();
        imFriendShipGroupMemberEntity.setGroupId(groupId);
        imFriendShipGroupMemberEntity.setToId(toId);
        try {
            int insert = imFriendShipGroupMemberMapper.insert(imFriendShipGroupMemberEntity);
            return insert;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int deleteGroupMember(Long groupId, String toId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("group_id", groupId);
        queryWrapper.eq("to_id", toId);

        try {
            int delete = imFriendShipGroupMemberMapper.delete(queryWrapper);
//            int insert = imFriendShipGroupMemberMapper.insert(imFriendShipGroupMemberEntity);
            return delete;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int clearGroupMember(Long groupId) {
        QueryWrapper<ImFriendShipGroupMemberEntity> query = new QueryWrapper<>();
        query.eq("group_id", groupId);
        int delete = imFriendShipGroupMemberMapper.delete(query);
        return delete;
    }
}
