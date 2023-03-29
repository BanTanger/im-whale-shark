package com.bantanger.im.domain.user.service;

import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.domain.user.model.req.*;
import com.bantanger.im.domain.user.model.resp.GetUserInfoResp;
import com.bantanger.im.common.ResponseVO;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
public interface ImUserService {

    /**
     * 批量导入用户信息
     * @param req
     * @return
     */
    ResponseVO importUser(ImportUserReq req);

    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    /**
     * 获取单个用户信息
     * @param userId
     * @param appId
     * @return
     */
    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId , Integer appId);

    ResponseVO deleteUser(DeleteUserReq req);

    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    ResponseVO login(LoginReq req);
}
