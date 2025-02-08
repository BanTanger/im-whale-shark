package com.bantanger.im.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * @description:
 * @author: lld
 * @version: 1.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * APP ID
     */
    @NotNull(message = "平台 Id 不能为空")
    private Integer appId;

    /**
     * 操作人，谁在调用接口
     */
    @NotBlank(message = "操作人 Id 不能为空")
    private String operater;

    private Integer clientType;

    private String imei;
}
