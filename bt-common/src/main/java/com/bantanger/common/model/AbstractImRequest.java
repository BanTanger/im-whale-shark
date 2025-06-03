package com.bantanger.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class AbstractImRequest implements Request {

    @NotNull(message = "平台 Id 不能为空")
    private Integer appId;

    @NotBlank(message = "操作人 Id 不能为空")
    private String operator;

    /**
     * 端标识：web端、pc端、移动端
     */
    private Integer clientType;

    private String imei;

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Integer getClientType() {
        return clientType;
    }

    public void setClientType(Integer clientType) {
        this.clientType = clientType;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }
}
