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

    /**
     * 获取所有正常用户信息
     * @param req
     * @return
     */
    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    /**
     * 获取单个正常用户信息
     * @param userId
     * @param appId
     * @return
     */
    ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId , Integer appId);

    /**
     * 删除正常用户
     * @param req
     * @return
     */
    ResponseVO deleteUser(DeleteUserReq req);

    /**
     * 修改正常用户信息
     * @param req
     * @return
     */
    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    /**
     * 用户登录功能
     * @param req
     * @return
     */
    ResponseVO login(LoginReq req);

    /**
     * 客户端向服务端请求该用户各接口需要拉取的数量
     * @param req
     * @return
     */
    ResponseVO getUserSequence(GetUserSequenceReq req);

}
