package com.bantanger.im.domain.friendship.model.resp;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/18 21:02
 */
@Data
public class CheckFriendShipResp {

    private String fromId;

    private String toId;

    /**
     * 校验状态，根据双向校验和单向校验有不同的status
     *
     * 单向校验：
     * 1 from 添加了 to，不确定 to 是否添加了 from --> CheckResult_single_Type_AWithB；
     * 0 from 没有添加 to，也不确定 to 有没有添加 from --> CheckResult_single_Type_NoRelation
     *
     * 双向校验
     * 1 from 添加了 to，to 也添加了 from --> CheckResult_Type_BothWay
     * 2 from 添加了 to，to 没有添加 from --> CheckResult_Both_Type_AWithB
     * 3 from 没有添加 to，to 添加了 from --> CheckResult_Both_Type_BWithA
     * 4 双方都没有添加 --> CheckResult_Both_Type_NoRelation
     *
     * 单向校验黑名单：
     * 1 from 没有拉黑 to，不确定 to 是否拉黑了 from --> CheckResult_singe_Type_AWithB；
     * 0 from 拉黑 to，不确定 to 是佛拉黑 from --> CheckResult_singe_Type_NoRelation
     *
     * 双向校验黑名单
     * 1 from 没有拉黑 to，to 也没有拉黑 from --> CheckResult_Type_BothWay
     * 2 from 没有拉黑 to，to 拉黑 from --> CheckResult_Both_Type_AWithB
     * 3 from 拉黑了 to，to 没有拉黑 from --> CheckResult_Both_Type_BWithA
     * 4 双方都拉黑 --> CheckResult_Both_Type_NoRelation
     */
    private Integer status;

}
