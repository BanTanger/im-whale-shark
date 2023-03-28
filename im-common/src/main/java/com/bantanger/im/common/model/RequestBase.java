package com.bantanger.im.common.model;

import lombok.Data;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
public class RequestBase {

    /**
     * APP ID
     */
    private Integer appId;

    /**
     * 操作人，谁在调用接口
     */
    private String operater;

    private Integer clientType;

    private String imei;
}
