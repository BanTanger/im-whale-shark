package com.bantanger.im.domain.group.service;

import com.bantanger.im.domain.group.model.req.*;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.group.dao.ImGroupEntity;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
public interface ImGroupService {

    /**
     * 批量导入群组信息
     * 如果 req 没有群组 ID，会通过 uuid 自动生成一个
     * @param req
     * @return
     */
    ResponseVO importGroup(ImportGroupReq req);

    /**
     * 创建群组
     * @param req
     * @return
     */
    ResponseVO createGroup(CreateGroupReq req);

    /**
     * 修改群基础信息
     * 如果是后台管理员调用，则不检查权限，如果不是则检查权限，
     * 如果是私有群（微信群）任何人都可以修改资料，公开群只有管理员可以修改
     * 如果是群主或者管理员可以修改其他信息。
     * @param req
     * @return com.lld.im.common.ResponseVO
     */
    ResponseVO updateBaseGroupInfo(UpdateGroupReq req);

    /**
     * 获取用户加入所有群组列表
     * @param req
     * @return com.lld.im.common.ResponseVO
     */
    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    /**
     * 解散群组，只支持后台管理员和群主解散, 私有群只能通过 app 管理员解散
     * @param req
     * @return com.lld.im.common.ResponseVO
     */
    ResponseVO destroyGroup(DestroyGroupReq req);

    /**
     * 转让群组
     * @param req
     * @return
     */
    ResponseVO transferGroup(TransferGroupReq req);

    /**
     * 获取指定群组信息
     * @param groupId
     * @param appId
     * @return 200 代表存在，4000 代表不存在
     */
    ResponseVO<ImGroupEntity> getGroup(String groupId, Integer appId);

    /**
     * 获取指定群组信息以及群内所有成员信息
     * @param req
     * @return
     */
    ResponseVO getGroup(GetGroupReq req);

    ResponseVO muteGroup(MuteGroupReq req);

}
