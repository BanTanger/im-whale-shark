package com.bantanger.im.domain.message.service.check;

import com.bantanger.im.common.ResponseVO;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/4 22:36
 */
public interface CheckSendMessage {

    /**
     * 检查发送人是否被禁言或者是禁用
     *
     * @param fromId
     * @param appId
     * @return
     */
    ResponseVO checkSenderForbidAndMute(String fromId, Integer appId);

    /**
     * 检查好友关系链
     *
     * @param fromId 己方
     * @param toId   对方
     * @param appId  平台 SDK
     * @return
     */
    ResponseVO checkFriendShip(String fromId, String toId, Integer appId);

    /**
     * 检查群组是否能发送消息
     *
     * @param fromId
     * @param groupId
     * @param appId
     * @return
     */
    ResponseVO checkGroupMessage(String fromId, String groupId, Integer appId);

}
