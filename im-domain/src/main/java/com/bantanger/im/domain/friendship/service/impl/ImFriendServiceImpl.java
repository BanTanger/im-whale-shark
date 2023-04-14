package com.bantanger.im.domain.friendship.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.friendship.*;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.FriendshipEventCommand;
import com.bantanger.im.common.enums.friend.FriendShipErrorCode;
import com.bantanger.im.common.enums.friend.FriendShipStatusEnum;
import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.common.model.SyncResp;
import com.bantanger.im.domain.friendship.dao.ImFriendShipEntity;
import com.bantanger.im.domain.friendship.dao.mapper.ImFriendShipMapper;
import com.bantanger.im.domain.friendship.model.req.*;
import com.bantanger.im.domain.friendship.model.req.callback.AddFriendAfterCallbackDto;
import com.bantanger.im.domain.friendship.model.req.callback.AddFriendBlackAfterCallbackDto;
import com.bantanger.im.domain.friendship.model.req.callback.DeleteFriendAfterCallbackDto;
import com.bantanger.im.domain.friendship.model.req.friend.*;
import com.bantanger.im.domain.friendship.service.ImFriendService;
import com.bantanger.im.domain.message.seq.RedisSequence;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.service.callback.CallbackService;
import com.bantanger.im.service.config.AppConfig;
import com.bantanger.im.service.sendmsg.MessageProducer;
import com.bantanger.im.service.utils.UserSequenceRepository;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.friend.AllowFriendTypeEnum;
import com.bantanger.im.common.enums.friend.CheckFriendShipTypeEnum;
import com.bantanger.im.common.exception.ApplicationException;
import com.bantanger.im.common.model.RequestBase;
import com.bantanger.im.domain.friendship.model.resp.CheckFriendShipResp;
import com.bantanger.im.domain.friendship.model.resp.ImportFriendShipResp;
import com.bantanger.im.domain.friendship.service.ImFriendShipRequestService;
import com.bantanger.im.domain.user.service.ImUserService;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@Service
public class ImFriendServiceImpl implements ImFriendService {

    @Resource
    ImFriendShipMapper imFriendShipMapper;

    @Resource
    ImUserService imUserServiceImpl;

    @Resource
    ImFriendService imFriendServiceImpl;

    @Resource
    ImFriendShipRequestService imFriendShipRequestServiceImpl;

    @Resource
    AppConfig appConfig;

    @Resource
    CallbackService callbackServiceImpl;

    @Resource
    MessageProducer messageProducer;

    @Resource
    RedisSequence redisSequence;

    @Resource
    UserSequenceRepository userSequenceRepository;

    @Override
    public ResponseVO importFriendShip(ImportFriendShipReq req) {

        if (req.getFriendItem().size() > 100) {
            return ResponseVO.errorResponse(FriendShipErrorCode.IMPORT_SIZE_BEYOND);
        }
        ImportFriendShipResp resp = new ImportFriendShipResp();
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        for (ImportFriendShipReq.ImportFriendDto dto : req.getFriendItem()) {
            ImFriendShipEntity entity = new ImFriendShipEntity();
            BeanUtils.copyProperties(dto, entity);
            entity.setAppId(req.getAppId());
            entity.setFromId(req.getFromId());
            try {
                int insert = imFriendShipMapper.insert(entity);
                if (insert == 1) {
                    successId.add(dto.getToId());
                } else {
                    errorId.add(dto.getToId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorId.add(dto.getToId());
            }
        }

        resp.setErrorId(errorId);
        resp.setSuccessId(successId);

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO addFriend(AddFriendReq req) {

        ResponseVO<ImUserDataEntity> fromInfo = imUserServiceImpl.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }
        ResponseVO<ImUserDataEntity> toInfo = imUserServiceImpl.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }

        // 事件执行前且选择开启回调
        if (appConfig.isAddFriendBeforeCallback()) {
            ResponseVO responseVO = callbackServiceImpl.beforeCallback(req.getAppId(),
                    Constants.CallbackCommand.AddFriendBefore,
                    JSONObject.toJSONString(req));
            if (!responseVO.isOk()) {
                // 如果回调不成功(状态码非 200), 错误需要返回给前端
                // 注意: 这里的回调不成功是指响应失败，表明用户没有该权限。
                // 回调机制抛出异常需要放行，正常处理，表名服务器后台故障，需要维修
                return responseVO;
            }
        }

        ImUserDataEntity data = toInfo.getData();
        if (data.getFriendAllowType() != null && data.getFriendAllowType() == AllowFriendTypeEnum.NOT_NEED.getCode()) {
            // 被加用户未设置好友申请认证，直接走添加逻辑
            return this.doAddFriend(req, req.getFromId(), req.getToItem(), req.getAppId());
        } else {
            // 被加用户设置好友申请认证，走申请逻辑(im_friendship_request)
            QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
            query.eq("app_id", req.getAppId());
            query.eq("from_id", req.getFromId());
            query.eq("to_id", req.getToItem().getToId());
            ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
            if (fromItem == null || fromItem.getStatus()
                    != FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                //插入一条好友申请的数据
                ResponseVO responseVO = imFriendShipRequestServiceImpl.addFienshipRequest(req.getFromId(), req.getToItem(), req.getAppId());
                if (!responseVO.isOk()) {
                    return responseVO;
                }
            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO updateFriend(UpdateFriendReq req) {

        ResponseVO<ImUserDataEntity> fromInfo = imUserServiceImpl.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserServiceImpl.getSingleUserInfo(req.getToItem().getToId(), req.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }

        ResponseVO responseVO = this.doUpdate(req.getFromId(), req.getToItem(), req.getAppId());
        if (responseVO.isOk()) {
            UpdateFriendPack updateFriendPack = new UpdateFriendPack();
            updateFriendPack.setRemark(req.getToItem().getRemark());
            updateFriendPack.setToId(req.getToItem().getToId());
            messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_UPDATE, updateFriendPack,
                    req.getAppId(), req.getClientType(), req.getImei());

            if (appConfig.isModifyFriendAfterCallback()) {
                AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToItem(req.getToItem());
                callbackServiceImpl.beforeCallback(req.getAppId(),
                        Constants.CallbackCommand.UpdateFriendAfter, JSONObject
                                .toJSONString(callbackDto));
            }
        }
        return responseVO;
    }

    @Transactional
    public ResponseVO doUpdate(String fromId, FriendDto dto, Integer appId) {
        long seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FriendShipSeq);
        UpdateWrapper<ImFriendShipEntity> updateWrapper = new UpdateWrapper<>();
        updateWrapper.lambda().set(ImFriendShipEntity::getAddSource, dto.getAddSource())
                .set(ImFriendShipEntity::getExtra, dto.getExtra())
                .set(ImFriendShipEntity::getRemark, dto.getRemark())
                .eq(ImFriendShipEntity::getAppId, appId)
                .eq(ImFriendShipEntity::getToId, dto.getToId())
                .eq(ImFriendShipEntity::getFromId, fromId);

        int update = imFriendShipMapper.update(null, updateWrapper);
        if (update == 1) {
            userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FriendShipSeq, seq);
            return ResponseVO.successResponse();
        }

        return ResponseVO.errorResponse();
    }

    @Override
    @Transactional
    public ResponseVO doAddFriend(RequestBase requestBase, String fromId, FriendDto dto, Integer appId) {
        // TODO 待重构优化
        //A-B
        //Friend 表插入 A 和 B 两条记录
        //查询是否有记录存在，如果存在则判断状态，如果是已添加，则提示已添加，如果是未添加，则修改状态

        // Friend 表插入 A 记录
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", appId);
        query.eq("from_id", fromId);
        query.eq("to_id", dto.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
        long seq = 0L;
        if (fromItem == null) {
            //走添加逻辑。
            fromItem = getFriendShipEntity(appId, fromId, dto.getToId(), dto);
            seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FriendShipSeq);
            fromItem.setFriendSequence(seq);
            int insert = imFriendShipMapper.insert(fromItem);
            if (insert != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FriendShipSeq, seq);
        } else {
            //如果存在则判断状态，如果是已添加，则提示已添加，如果是未添加，则修改状态
            if (fromItem.getStatus() == FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_YOUR_FRIEND);
            } else {
                ImFriendShipEntity update = new ImFriendShipEntity();

                if (StringUtils.isNotBlank(dto.getAddSource())) {
                    update.setAddSource(dto.getAddSource());
                }

                if (StringUtils.isNotBlank(dto.getRemark())) {
                    update.setRemark(dto.getRemark());
                }

                if (StringUtils.isNotBlank(dto.getExtra())) {
                    update.setExtra(dto.getExtra());
                }
                seq = redisSequence.doGetSeq(appId + ":" + Constants.SeqConstants.FriendShipSeq);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

                int result = imFriendShipMapper.update(update, query);
                if (result != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
                }
                userSequenceRepository.writeUserSeq(appId, fromId, Constants.SeqConstants.FriendShipSeq, seq);
            }

        }

        // Friend 表插入 B 记录
        QueryWrapper<ImFriendShipEntity> toQuery = new QueryWrapper<>();
        toQuery.eq("app_id", appId);
        toQuery.eq("from_id", dto.getToId());
        toQuery.eq("to_id", fromId);
        ImFriendShipEntity toItem = imFriendShipMapper.selectOne(toQuery);
        if (toItem == null) {
            toItem = getFriendShipEntity(appId, dto.getToId(), fromId, dto);
            toItem.setFriendSequence(seq);
            int insert = imFriendShipMapper.insert(toItem);
            userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FriendShipSeq, seq);
        } else {
            if (!Objects.equals(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode(), toItem.getStatus())) {
                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
                imFriendShipMapper.update(update, toQuery);
                userSequenceRepository.writeUserSeq(appId, dto.getToId(), Constants.SeqConstants.FriendShipSeq, seq);
            }
        }

        // TCP 通知发送给 from 端
        AddFriendPack addFriendPack = new AddFriendPack();
        addFriendPack.setFromId(fromItem.getFromId());
        addFriendPack.setRemark(fromItem.getRemark());
        addFriendPack.setToId(fromItem.getToId());
        addFriendPack.setSequence(seq);
        addFriendPack.setAddSource(fromItem.getAddSource());

        if (requestBase != null) {
            // 存在 req 同步除本端的所有端
            messageProducer.sendMsgToUser(fromId, FriendshipEventCommand.FRIEND_ADD, addFriendPack,
                    requestBase.getAppId(), requestBase.getClientType(), requestBase.getImei());
        } else {
            // 没有 req 直接同步到所有端
            messageProducer.sendToUserAllClient(fromId,
                    FriendshipEventCommand.FRIEND_ADD, addFriendPack, appId);
        }

        // TCP 通知发给 to 端
        AddFriendPack addFriendToPack = new AddFriendPack();
        addFriendToPack.setFromId(toItem.getFromId());
        addFriendToPack.setRemark(toItem.getRemark());
        addFriendToPack.setToId(toItem.getToId());
        addFriendToPack.setSequence(seq);
        addFriendToPack.setAddSource(toItem.getAddSource());

        // 同步所有端
        messageProducer.sendToUserAllClient(toItem.getFromId(),
                FriendshipEventCommand.FRIEND_ADD, addFriendToPack, appId);

        // 事件执行后且选择开启回调
        if (appConfig.isAddFriendAfterCallback()) {
            AddFriendAfterCallbackDto callbackDto = new AddFriendAfterCallbackDto();
            callbackDto.setFromId(fromId);
            callbackDto.setToItem(dto);
            callbackServiceImpl.afterCallback(appId,
                    Constants.CallbackCommand.AddFriendAfter,
                    JSONObject.toJSONString(callbackDto));
        }
        return ResponseVO.successResponse();
    }

    private ImFriendShipEntity getFriendShipEntity(Integer appId, String fromId, String toId, FriendDto dto) {
        ImFriendShipEntity userItem = new ImFriendShipEntity();
        userItem.setAppId(appId);
        userItem.setFromId(fromId);
        userItem.setToId(toId);
        userItem.setRemark(dto.getRemark());
        userItem.setAddSource(dto.getAddSource());
        userItem.setExtra(dto.getExtra());
        userItem.setStatus(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());
        userItem.setCreateTime(System.currentTimeMillis());
        return userItem;
    }

    @Override
    public ResponseVO deleteFriend(DeleteFriendReq req) {

        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("to_id", req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
        if (fromItem == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.TO_IS_NOT_YOUR_FRIEND);
        } else {
            if (fromItem.getStatus() != null &&
                    Objects.equals(fromItem.getStatus(),
                            FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode())) {

                ImFriendShipEntity update = new ImFriendShipEntity();
                long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipSeq);
                update.setFriendSequence(seq);
                update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
                imFriendShipMapper.update(update, query);
                userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendShipSeq, seq);

                // TCP 通知
                DeleteFriendPack deleteFriendPack = new DeleteFriendPack();
                deleteFriendPack.setFromId(req.getFromId());
                deleteFriendPack.setToId(req.getToId());
                deleteFriendPack.setSequence(seq);
                messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_DELETE, deleteFriendPack,
                        req.getAppId(), req.getClientType(), req.getImei());

                //之后回调
                if (appConfig.isAddFriendAfterCallback()) {
                    DeleteFriendAfterCallbackDto callbackDto = new DeleteFriendAfterCallbackDto();
                    callbackDto.setFromId(req.getFromId());
                    callbackDto.setToId(req.getToId());
                    callbackServiceImpl.afterCallback(req.getAppId(),
                            Constants.CallbackCommand.DeleteFriendAfter,
                            JSONObject.toJSONString(callbackDto));
                }

            } else {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }
        }
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteAllFriend(DeleteFriendReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("status", FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode());

        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setStatus(FriendShipStatusEnum.FRIEND_STATUS_DELETE.getCode());
        imFriendShipMapper.update(update, query);

        DeleteAllFriendPack deleteFriendPack = new DeleteAllFriendPack();
        deleteFriendPack.setFromId(req.getFromId());
        messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_ALL_DELETE,
                deleteFriendPack, req.getAppId(), req.getClientType(), req.getImei());

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getAllFriendShip(GetAllFriendShipReq req) {
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        return ResponseVO.successResponse(imFriendShipMapper.selectList(query));
    }

    @Override
    public ResponseVO getRelation(GetRelationReq req) {

        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("to_id", req.getToId());

        ImFriendShipEntity entity = imFriendShipMapper.selectOne(query);
        if (entity == null) {
            return ResponseVO.errorResponse(FriendShipErrorCode.REPEATSHIP_IS_NOT_EXIST);
        }
        return ResponseVO.successResponse(entity);
    }

    @Override
    public ResponseVO checkBlck(CheckFriendShipReq req) {

        Map<String, Integer> toIdMap
                = req.getToIds().stream().collect(Collectors
                .toMap(Function.identity(), s -> 0));
        List<CheckFriendShipResp> result = new ArrayList<>();
        if (req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            result = imFriendShipMapper.checkFriendShipBlack(req);
        } else {
            result = imFriendShipMapper.checkFriendShipBlackBoth(req);
        }

        Map<String, Integer> collect = result.stream()
                .collect(Collectors
                        .toMap(CheckFriendShipResp::getToId,
                                CheckFriendShipResp::getStatus));
        for (String toId :
                toIdMap.keySet()) {
            if (!collect.containsKey(toId)) {
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setStatus(toIdMap.get(toId));
                result.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(result);
    }

    @Override
    public ResponseVO addBlack(AddFriendShipBlackReq req) {

        ResponseVO<ImUserDataEntity> fromInfo = imUserServiceImpl.getSingleUserInfo(req.getFromId(), req.getAppId());
        if (!fromInfo.isOk()) {
            return fromInfo;
        }

        ResponseVO<ImUserDataEntity> toInfo = imUserServiceImpl.getSingleUserInfo(req.getToId(), req.getAppId());
        if (!toInfo.isOk()) {
            return toInfo;
        }
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("from_id", req.getFromId());
        query.eq("to_id", req.getToId());

        long seq = 0L;
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(query);
        if (fromItem == null) {
            //走添加逻辑。
            seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipSeq);

            fromItem = new ImFriendShipEntity();
            fromItem.setFromId(req.getFromId());
            fromItem.setToId(req.getToId());
            fromItem.setAppId(req.getAppId());
            fromItem.setFriendSequence(seq);
            fromItem.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
            fromItem.setCreateTime(System.currentTimeMillis());
            int insert = imFriendShipMapper.insert(fromItem);
            if (insert != 1) {
                return ResponseVO.errorResponse(FriendShipErrorCode.ADD_FRIEND_ERROR);
            }
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendShipSeq, seq);
        } else {
            //如果存在则判断状态，如果是拉黑，则提示已拉黑，如果是未拉黑，则修改状态
            if (fromItem.getBlack() != null && fromItem.getBlack() == FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode()) {
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
            } else {
                seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipSeq);

                ImFriendShipEntity update = new ImFriendShipEntity();
                update.setBlack(FriendShipStatusEnum.BLACK_STATUS_BLACKED.getCode());
                int result = imFriendShipMapper.update(update, query);
                if (result != 1) {
                    return ResponseVO.errorResponse(FriendShipErrorCode.ADD_BLACK_ERROR);
                }
                userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendShipSeq, seq);
            }
        }

        // 发送 TCP 通知
        AddFriendBlackPack addFriendBlackPack = new AddFriendBlackPack();
        addFriendBlackPack.setFromId(req.getFromId());
        addFriendBlackPack.setToId(req.getToId());
        addFriendBlackPack.setSequence(seq);
        messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_ADD, addFriendBlackPack,
                req.getAppId(), req.getClientType(), req.getImei());

        //之后回调
        if (appConfig.isAddFriendShipBlackAfterCallback()) {
            AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
            callbackDto.setFromId(req.getFromId());
            callbackDto.setToId(req.getToId());
            callbackServiceImpl.afterCallback(req.getAppId(),
                    Constants.CallbackCommand.AddBlackAfter,
                    JSONObject.toJSONString(callbackDto));
        }

        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO deleteBlack(DeleteBlackReq req) {
        QueryWrapper queryFrom = new QueryWrapper<>()
                .eq("from_id", req.getFromId())
                .eq("app_id", req.getAppId())
                .eq("to_id", req.getToId());
        ImFriendShipEntity fromItem = imFriendShipMapper.selectOne(queryFrom);
        if (fromItem.getBlack() != null && fromItem.getBlack() == FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()) {
            throw new ApplicationException(FriendShipErrorCode.FRIEND_IS_NOT_YOUR_BLACK);
        }

        long seq = redisSequence.doGetSeq(req.getAppId() + ":" + Constants.SeqConstants.FriendShipSeq);

        ImFriendShipEntity update = new ImFriendShipEntity();
        update.setBlack(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode());
        int update1 = imFriendShipMapper.update(update, queryFrom);
        if (update1 == 1) {
            userSequenceRepository.writeUserSeq(req.getAppId(), req.getFromId(), Constants.SeqConstants.FriendShipSeq, seq);

            // 发送 TCP 通知
            DeleteBlackPack deleteFriendPack = new DeleteBlackPack();
            deleteFriendPack.setFromId(req.getFromId());
            deleteFriendPack.setToId(req.getToId());
            deleteFriendPack.setSequence(seq);
            messageProducer.sendMsgToUser(req.getFromId(), FriendshipEventCommand.FRIEND_BLACK_DELETE,
                    deleteFriendPack, req.getAppId(), req.getClientType(), req.getImei());

            //之后回调
            if (appConfig.isAddFriendShipBlackAfterCallback()) {
                AddFriendBlackAfterCallbackDto callbackDto = new AddFriendBlackAfterCallbackDto();
                callbackDto.setFromId(req.getFromId());
                callbackDto.setToId(req.getToId());
                callbackServiceImpl.afterCallback(req.getAppId(),
                        Constants.CallbackCommand.DeleteBlack, JSONObject
                                .toJSONString(callbackDto));
            }
            return ResponseVO.successResponse();
        }
        return ResponseVO.errorResponse();
    }

    @Override
    public ResponseVO checkFriendship(CheckFriendShipReq req) {

        Map<String, Integer> res = req.getToIds().stream()
                .collect(Collectors.toMap(
                        /* key: 每一个 toId */
                        Function.identity(),
                        /* value: 0 */
                        s -> 0));

        List<CheckFriendShipResp> resp = new ArrayList<>();

        if (req.getCheckType() == CheckFriendShipTypeEnum.SINGLE.getType()) {
            resp = imFriendShipMapper.checkFriendShip(req);
        } else {
            resp = imFriendShipMapper.checkFriendShipBoth(req);
        }

        Map<String, Integer> collect = resp.stream()
                .collect(Collectors.toMap(
                        /* key: 每一个 toId*/
                        CheckFriendShipResp::getToId,
                        /* value: 每一个 toId 与 fromId 的状态 */
                        CheckFriendShipResp::getStatus));

        for (String toId : res.keySet()) {
            // 如果没有出现 toId，将其填充
            if (!collect.containsKey(toId)) {
                CheckFriendShipResp checkFriendShipResp = new CheckFriendShipResp();
                checkFriendShipResp.setFromId(req.getFromId());
                checkFriendShipResp.setToId(toId);
                checkFriendShipResp.setStatus(res.get(toId));
                resp.add(checkFriendShipResp);
            }
        }

        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO syncFriendShipList(SyncReq req) {
        if (req.getMaxLimit() > appConfig.getFriendShipMaxCount()) {
            // 前端传输限制，保证一次增量拉取数据量不超过配置文件的值
            req.setMaxLimit(appConfig.getFriendShipMaxCount());
        }

        SyncResp<ImFriendShipEntity> resp = new SyncResp<>();
        // server_seq > req(client)_seq limit maxLimit;
        QueryWrapper<ImFriendShipEntity> query = new QueryWrapper<>();
        query.eq("from_id", req.getOperater());
        query.gt("friend_sequence", req.getLastSequence());
        query.eq("app_id", req.getAppId());
        query.last("limit " + req.getMaxLimit());
        query.orderByAsc("friend_sequence");
        List<ImFriendShipEntity> list = imFriendShipMapper.selectList(query);

        if (!CollectionUtils.isEmpty(list)) {
            ImFriendShipEntity maxSeqEntity = list.get(list.size() - 1);
            resp.setDataList(list);
            // 设置最大 Seq
            Long friendShipMaxSeq = imFriendShipMapper
                    .getFriendShipMaxSeq(req.getAppId());
            resp.setMaxSequence(friendShipMaxSeq);
            // 设置是否拉取完毕
            resp.setCompleted(maxSeqEntity.getFriendSequence() >= friendShipMaxSeq);
            return ResponseVO.successResponse(resp);
        }
        resp.setCompleted(true);
        return ResponseVO.successResponse();
    }

}
