package com.bantanger.im.common.model;

import lombok.Data;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/13 23:33
 */
@Data
public class SyncReq extends RequestBase {

    /** 客户端最大 Seq */
    private Long lastSequence;

    /** 一次性最大拉取次数 */
    private Integer maxLimit;

}
