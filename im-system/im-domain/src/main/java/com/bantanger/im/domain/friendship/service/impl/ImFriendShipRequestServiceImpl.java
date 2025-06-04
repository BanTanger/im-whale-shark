package com.bantanger.im.domain.friendship.service.impl;

import com.bantanger.im.codec.pack.friendship.ApproverFriendRequestPack;
import com.bantanger.im.codec.pack.friendship.ReadAllFriendRequestPack;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.FriendshipEventCommand;
import com.bantanger.im.common.enums.friend.ApproverFriendRequestStatusEnum;
import com.bantanger.im.common.enums.friend.FriendShipErrorCode;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.domain.friendship.dao.ImFriendShipRequestEntity;
import com.bantanger.im.domain.friendship.dao.mapper.ImFriendShipRequestMapper;
import com.bantanger.im.domain.friendship.model.req.ApprovalFriendRequestReq;
import com.bantanger.im.domain.friendship.model.req.friend.FriendDto;
import com.bantanger.im.domain.friendship.model.req.friend.ReadFriendShipRequestReq;
import com.bantanger.im.domain.friendship.service.ImFriendService;
import com.bantanger.im.domain.friendship.service.ImFriendShipRequestService;
import com.bantanger.im.domain.messageddd.domainservice.sendmsg.MessageProducer;
import com.bantanger.im.infrastructure.support.ids.SequenceIdWorker;
import com.bantanger.im.infrastructure.utils.UserSequenceRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;

@Service
public class ImFriendShipRequestServiceImpl implements ImFriendShipRequestService {

    @Resource
    private ImFriendShipRequestMapper imFriendShipRequestMapper;
    @Resource
    private ImFriendService imFriendShipService;
    @Resource
    private MessageProducer messageProducer;
    @Resource
    private SequenceIdWorker sequenceIdWorker;
    @Resource
    private UserSequenceRepository userSequenceRepository;

    @Override
    public ResponseVO getFriendRequest(String fromId, Integer appId) {

        List<ImFriendShipRequestEntity> requestList = imFriendShipRequestMapper.selectList(
                new LambdaQueryWrapper<ImFriendShipRequestEntity>()
                        .eq(ImFriendShipRequestEntity::getAppId, appId)
                        .eq(ImFriendShipRequestEntity::getToId, fromId));

        return ResponseVO.successResponse(requestList);
    }

    //A + B
    @Override
    public ResponseVO addFriendshipRequest(String fromId, FriendDto dto, Integer appId) {

        ImFriendShipRequestEntity request = imFriendShipRequestMapper.selectOne(
                new LambdaQueryWrapper<ImFriendShipRequestEntity>()
                        .eq(ImFriendShipRequestEntity::getAppId, appId)
                        .eq(ImFriendShipRequestEntity::getFromId, fromId)
                        .eq(ImFriendShipRequestEntity::getToId, dto.getToId()));

        long seq = sequenceIdWorker.doGetSeq(appId + ":" +
                Constants.SeqConstants.FriendShipRequestSeq);

        if (request == null) {
            // 第一次添加，插入申请结果
            request = new ImFriendShipRequestEntity();
            request.setAddSource(dto.getAddSource());
            request.setAddWording(dto.getAddWording());
            request.setAppId(appId);
            request.setFromId(fromId);
            request.setToId(dto.getToId());
            request.setReadStatus(0);
            request.setApproveStatus(0);
            request.setSequence(seq);
            request.setRemark(dto.getRemark());
            request.setCreateTime(System.currentTimeMillis());
            imFriendShipRequestMapper.insert(request);
        } else {
            //修改记录内容和更新时间
            if (StringUtils.isNotBlank(dto.getAddSource())) {
                request.setAddWording(dto.getAddWording());
            }
            if (StringUtils.isNotBlank(dto.getRemark())) {
                request.setRemark(dto.getRemark());
            }
            if (StringUtils.isNotBlank(dto.getAddWording())) {
                request.setAddWording(dto.getAddWording());
            }
            request.setSequence(seq);
            request.setApproveStatus(0);
            request.setReadStatus(0);
            imFriendShipRequestMapper.updateById(request);
        }

        userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FriendShipRequestSeq, seq);

        //发送好友申请的 tcp 给接收方
        messageProducer.sendToUserAllClient(dto.getToId(),
                FriendshipEventCommand.FRIEND_REQUEST, request, appId);

        return ResponseVO.successResponse();
    }

    @Override
    @Transactional
    public ResponseVO approvalFriendRequest(ApprovalFriendRequestReq req) {

        ImFriendShipRequestEntity imFriendShipRequestEntity = imFriendShipRequestMapper.selectOne(
                new LambdaQueryWrapper<ImFriendShipRequestEntity>()
                        .eq(ImFriendShipRequestEntity::getId, req.getId())
                        .eq(ImFriendShipRequestEntity::getApproveStatus, ApproverFriendRequestStatusEnum.NORMAL.getCode()));
        if (imFriendShipRequestEntity == null) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_REQUEST_IS_NOT_EXIST);
        }

        if (!req.getOperator().equals(imFriendShipRequestEntity.getToId())) {
            //只能审批发给自己的好友请求
            throw new ApplicationException(FriendShipErrorCode.NOT_APPROVER_OTHER_MAN_REQUEST);
        }

        long seq = sequenceIdWorker.doGetSeq(req.getAppId() +
                ":" + Constants.SeqConstants.FriendShipRequestSeq);

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setApproveStatus(req.getStatus());
        update.setSequence(seq);
        update.setUpdateTime(System.currentTimeMillis());
        update.setId(req.getId());
        imFriendShipRequestMapper.updateById(update);

        userSequenceRepository.writeUserSeq(req.getAppId(),
                req.getOperator(), Constants.SeqConstants.FriendShipRequestSeq, seq);

        if (ApproverFriendRequestStatusEnum.AGREE.getCode() == req.getStatus()) {
            //同意 ===> 去执行添加好友逻辑
            FriendDto dto = new FriendDto();
            dto.setAddSource(imFriendShipRequestEntity.getAddSource());
            dto.setAddWording(imFriendShipRequestEntity.getAddWording());
            dto.setRemark(imFriendShipRequestEntity.getRemark());
            dto.setToId(imFriendShipRequestEntity.getToId());
            ResponseVO responseVO = imFriendShipService.doAddFriend(
                    req, imFriendShipRequestEntity.getFromId(), dto, req.getAppId());

            if (!responseVO.isOk() && responseVO.getCode() != FriendShipErrorCode.TO_IS_YOUR_FRIEND.getCode()) {
                return responseVO;
            }
        }

        //发送好友申请的 tcp 给接收方
        ApproverFriendRequestPack approverFriendRequestPack = new ApproverFriendRequestPack();
        approverFriendRequestPack.setId(req.getId());
        approverFriendRequestPack.setSequence(seq);
        approverFriendRequestPack.setStatus(req.getStatus());
        messageProducer.sendMsgToUser(imFriendShipRequestEntity.getToId(), FriendshipEventCommand.FRIEND_REQUEST_APPROVER,
                approverFriendRequestPack, req.getAppId(), req.getClientType(), req.getImei());
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO readFriendShipRequestReq(ReadFriendShipRequestReq req) {

        long seq = sequenceIdWorker.doGetSeq(req.getAppId() + ":" +
                Constants.SeqConstants.FriendShipRequestSeq);

        ImFriendShipRequestEntity update = new ImFriendShipRequestEntity();
        update.setReadStatus(1);
        update.setSequence(seq);
        imFriendShipRequestMapper.update(update, new LambdaUpdateWrapper<ImFriendShipRequestEntity>()
                .eq(ImFriendShipRequestEntity::getAppId, req.getAppId())
                .eq(ImFriendShipRequestEntity::getToId, req.getFromId()));

        userSequenceRepository.writeUserSeq(req.getAppId(),
                req.getOperator(), Constants.SeqConstants.FriendShipRequestSeq, seq);

        // TCP 通知
        ReadAllFriendRequestPack readAllFriendRequestPack = new ReadAllFriendRequestPack();
        readAllFriendRequestPack.setFromId(req.getFromId());
        readAllFriendRequestPack.setSequence(seq);
        messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_REQUEST_READ,
                readAllFriendRequestPack, req.getAppId(), req.getClientType(), req.getImei());

        return ResponseVO.successResponse();
    }

}
