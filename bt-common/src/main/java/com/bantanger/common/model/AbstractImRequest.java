package com.bantanger.common.model;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
public abstract class AbstractImRequest implements Request {

    private Integer appId;

    private String operator;

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
