package com.bantanger.im.domain.message.service;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.error.MessageErrorCode;
import com.bantanger.im.common.enums.friend.FriendShipErrorCode;
import com.bantanger.im.common.enums.friend.FriendShipStatusEnum;
import com.bantanger.im.common.enums.user.UserForbiddenFlagEnum;
import com.bantanger.im.common.enums.user.UserSilentFlagEnum;
import com.bantanger.im.domain.friendship.dao.ImFriendShipEntity;
import com.bantanger.im.domain.friendship.model.req.GetRelationReq;
import com.bantanger.im.domain.friendship.service.ImFriendService;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.domain.user.service.ImUserService;
import com.bantanger.im.service.config.AppConfig;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 9:31
 */
@Service
public class CheckSendMessageService {

    @Resource
    ImUserService userService;

    @Resource
    ImFriendService friendService;

    @Resource
    AppConfig appConfig;

    /**
     * 检查发送人是否被禁言或者是禁用
     *
     * @param fromId
     * @param appId
     * @return
     */
    public ResponseVO checkSenderForbidAndMute(String fromId, Integer appId) {
        // 查询用户是否存在
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(fromId, appId);
        if (!singleUserInfo.isOk()) {
            return singleUserInfo;
        }

        // 用户是否被禁言或禁用
        ImUserDataEntity user = singleUserInfo.getData();
        if (UserForbiddenFlagEnum.FORBIBBEN.getCode() == user.getForbiddenFlag()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_FORBIBBEN);
        } else if (UserSilentFlagEnum.MUTE.getCode() == user.getSilentFlag()) {
            return ResponseVO.errorResponse(MessageErrorCode.FROMER_IS_MUTE);
        }

        return ResponseVO.successResponse();
    }

    /**
     * 检查好友关系链
     * @param fromId 己方
     * @param toId 对方
     * @param appId 平台 SDK
     * @return
     */
    public ResponseVO checkFriendShip(String fromId, String toId, Integer appId) {

        if (appConfig.isSendMessageCheckFriend()) {
            // 自己与对方的好友关系链是否正常【库表是否有这行记录: from2to】
            ResponseVO<ImFriendShipEntity> fromRelation = getRelation(fromId, toId, appId);
            if (!fromRelation.isOk()) {
                return fromRelation;
            }
            // 对方与自己的好友关系链是否正常【库表是否有这行记录: to2from】
            ResponseVO<ImFriendShipEntity> toRelation = getRelation(toId, fromId, appId);
            if (!toRelation.isOk()) {
                return toRelation;
            }

            // 检查自己是否删除对方【status = 2（删除）】
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != fromRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED);
            }

            // 检查对方是否删除己方【status = 2（删除）】
            if(FriendShipStatusEnum.FRIEND_STATUS_NORMAL.getCode()
                    != toRelation.getData().getStatus()){
                return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_DELETED_YOU);
            }

            // 检查黑名单列表中
            if(appConfig.isSendMessageCheckBlack()){
                // 检查自己是否拉黑对方
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != fromRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.FRIEND_IS_BLACK);
                }

                // 检查对方是否拉黑自己
                if(FriendShipStatusEnum.BLACK_STATUS_NORMAL.getCode()
                        != toRelation.getData().getBlack()){
                    return ResponseVO.errorResponse(FriendShipErrorCode.TARGET_IS_BLACK_YOU);
                }
            }
        }
        return ResponseVO.successResponse();
    }

    private ResponseVO<ImFriendShipEntity> getRelation(String fromId, String toId, Integer appId) {
        GetRelationReq getRelationReq = new GetRelationReq();
        getRelationReq.setFromId(fromId);
        getRelationReq.setToId(toId);
        getRelationReq.setAppId(appId);
        ResponseVO<ImFriendShipEntity> relation = friendService.getRelation(getRelationReq);
        return relation;
    }
}
