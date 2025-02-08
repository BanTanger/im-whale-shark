package com.bantanger.im.domain.friendship.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.bantanger.im.codec.pack.friendship.AddFriendGroupPack;
import com.bantanger.im.codec.pack.friendship.DeleteFriendGroupPack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.FriendshipEventCommand;
import com.bantanger.im.common.enums.friend.FriendShipErrorCode;
import com.bantanger.im.common.model.ClientInfo;
import com.bantanger.im.domain.friendship.dao.ImFriendShipGroupEntity;
import com.bantanger.im.domain.friendship.dao.mapper.ImFriendShipGroupMapper;
import com.bantanger.im.domain.friendship.service.ImFriendShipGroupMemberService;
import com.bantanger.im.domain.message.seq.RedisSequence;
import com.bantanger.im.service.sendmsg.MessageProducer;
import com.bantanger.im.service.utils.UserSequenceRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.friend.DelFlagEnum;
import com.bantanger.im.domain.friendship.model.req.group.member.AddFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.model.req.group.AddFriendShipGroupReq;
import com.bantanger.im.domain.friendship.model.req.group.DeleteFriendShipGroupReq;
import com.bantanger.im.domain.friendship.service.ImFriendShipGroupService;
import com.bantanger.im.domain.user3.service.ImUserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

@Service
public class ImFriendShipGroupServiceImpl implements ImFriendShipGroupService {

    @Resource
    ImFriendShipGroupMapper imFriendShipGroupMapper;

    @Resource
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    @Resource
    ImUserService imUserService;

    @Resource
    MessageProducer messageProducer;

    @Resource
    RedisSequence redisSequence;

    @Resource
    UserSequenceRepository userSequenceRepository;

    @Override
    @Transactional
    public ResponseVO addGroup(AddFriendShipGroupReq req) {

        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", req.getGroupName());
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        if (entity != null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipGroupSeq);

        //写入db
        ImFriendShipGroupEntity insert = new ImFriendShipGroupEntity();
        insert.setAppId(req.getAppId());
        insert.setCreateTime(System.currentTimeMillis());
        insert.setDelFlag(DelFlagEnum.NORMAL.getCode());
        insert.setGroupName(req.getGroupName());
        insert.setFromId(req.getFromId());
        insert.setSequence(seq);
        try {
            int insert1 = imFriendShipGroupMapper.insert(insert);
            if (insert1 != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_CREATE_ERROR);
            }
            if (insert1 == 1 && CollectionUtil.isNotEmpty(req.getToIds())) {
                AddFriendShipGroupMemberReq addFriendShipGroupMemberReq = new AddFriendShipGroupMemberReq();
                addFriendShipGroupMemberReq.setFromId(req.getFromId());
                addFriendShipGroupMemberReq.setGroupName(req.getGroupName());
                addFriendShipGroupMemberReq.setToIds(req.getToIds());
                addFriendShipGroupMemberReq.setAppId(req.getAppId());
                imFriendShipGroupMemberService.addGroupMember(addFriendShipGroupMemberReq);
                return ResponseVO.successResponse();
            }
        } catch (DuplicateKeyException e) {
            e.getStackTrace();
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_EXIST);
        }

        // 发送 TCP 通知
        AddFriendGroupPack addFriendGropPack = new AddFriendGroupPack();
        addFriendGropPack.setFromId(req.getFromId());
        addFriendGropPack.setGroupName(req.getGroupName());
        addFriendGropPack.setSequence(seq);
        messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_ADD,
                addFriendGropPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

        userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendShipGroupSeq, seq);
        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO deleteGroup(DeleteFriendShipGroupReq req) {

        for (String groupName : req.getGroupName()) {
            QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
            query.eq("group_name", groupName);
            query.eq("app_id", req.getAppId());
            query.eq("from_id", req.getFromId());
            query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

            ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

            if (entity != null) {
                long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipGroupSeq);

                ImFriendShipGroupEntity update = new ImFriendShipGroupEntity();
                update.setSequence(seq);
                update.setGroupId(entity.getGroupId());
                update.setDelFlag(DelFlagEnum.DELETE.getCode());
                imFriendShipGroupMapper.updateById(update);
                imFriendShipGroupMemberService.clearGroupMember(entity.getGroupId());

                // 发送 TCP 通知
                DeleteFriendGroupPack deleteFriendGroupPack = new DeleteFriendGroupPack();
                deleteFriendGroupPack.setFromId(req.getFromId());
                deleteFriendGroupPack.setGroupName(groupName);
                deleteFriendGroupPack.setSequence(seq);
                messageProducer.sendToUserExceptClient(req.getFromId(), FriendshipEventCommand.FRIEND_GROUP_DELETE,
                        deleteFriendGroupPack,new ClientInfo(req.getAppId(),req.getClientType(),req.getImei()));

            }
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getGroup(String fromId, String groupName, Integer appId) {
        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", groupName);
        query.eq("app_id", appId);
        query.eq("from_id", fromId);
        query.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_SHIP_GROUP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public Long updateSeq(String fromId, String groupName, Integer appId) {
        QueryWrapper<ImFriendShipGroupEntity> query = new QueryWrapper<>();
        query.eq("group_name", groupName);
        query.eq("app_id", appId);
        query.eq("from_id", fromId);

        ImFriendShipGroupEntity entity = imFriendShipGroupMapper.selectOne(query);

        long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FriendShipGroupSeq);

        ImFriendShipGroupEntity group = new ImFriendShipGroupEntity();
        group.setGroupId(entity.getGroupId());
        group.setSequence(seq);
        imFriendShipGroupMapper.updateById(group);
        return seq;
    }

}
