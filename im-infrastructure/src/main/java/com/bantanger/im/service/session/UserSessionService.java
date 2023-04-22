package com.bantanger.im.service.session;

import com.bantanger.im.common.model.UserSession;

import java.util.List;

/**
 * 定义获取 UserSession 的接口
 * @author BanTanger 半糖
 * @Date 2023/3/31 20:07
 */
public interface UserSessionService {

    /**
     * 获取用户所有[在线]的 Session 信息
     * @param appId
     * @param userId
     * @return
     */
    List<UserSession> getUserSession(Integer appId, String userId);

    /**
     * 获取指定端的用户 Session 信息
     * 无论在线离线 TODO
     * @param appId
     * @param userId
     * @param clientType
     * @param imei
     * @return
     */
    UserSession getUserSession(Integer appId, String userId, Integer clientType, String imei);

}
