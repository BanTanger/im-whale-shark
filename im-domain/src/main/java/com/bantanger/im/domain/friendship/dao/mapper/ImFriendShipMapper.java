package com.bantanger.im.domain.friendship.dao.mapper;

import com.bantanger.im.domain.friendship.dao.ImFriendShipEntity;
import com.bantanger.im.domain.friendship.model.req.CheckFriendShipReq;
import com.bantanger.im.domain.friendship.model.resp.CheckFriendShipResp;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 好友关系链验证，参考腾讯云 https://cloud.tencent.com/document/product/269/1501
 * 设计两种好友检验方式(CheckType)：单向校验好友关系(CheckResult_Type_Single)、双向校验好友关系(CheckResult_Type_Both)
 * @author: bantanger 半糖
 */
@Mapper
public interface ImFriendShipMapper extends BaseMapper<ImFriendShipEntity> {

    /**
     * 单向校验：
     * 1 from 添加了 to，不确定 to 是否添加了 from --> CheckResult_single_Type_AWithB；
     * 0 from 没有添加 to，也不确定 to 有没有添加 from --> CheckResult_single_Type_NoRelation
     * @param req
     * @return
     */
    List<CheckFriendShipResp> checkFriendShip(CheckFriendShipReq req);

    /**
     * 双向校验
     * 1 from 添加了 to，to 也添加了 from --> CheckResult_Type_BothWay
     * 2 from 添加了 to，to 没有添加 from --> CheckResult_Both_Type_AWithB
     * 3 from 没有添加 to，to 添加了 from --> CheckResult_Both_Type_BWithA
     * 4 双方都没有添加 --> CheckResult_Both_Type_NoRelation
     * @param toId
     * @return
     */
    List<CheckFriendShipResp> checkFriendShipBoth(CheckFriendShipReq toId);

    /**
     *
     * @param req
     * @return
     */
    List<CheckFriendShipResp> checkFriendShipBlack(CheckFriendShipReq req);

    List<CheckFriendShipResp> checkFriendShipBlackBoth(CheckFriendShipReq toId);

}
