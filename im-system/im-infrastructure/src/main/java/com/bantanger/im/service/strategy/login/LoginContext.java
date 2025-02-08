package com.bantanger.im.service.strategy.login;

import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.strategy.login.impl.AllClientLoginStatus;

/**
 * 登录上下文，控制状态切换和状态行为调用
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:20
 */
public class LoginContext {

    private LoginStatus status;

    public LoginContext() {
        status = new AllClientLoginStatus();
        status.setContext(this);
    }

    public void setStatus(LoginStatus status) {
        this.status = status;
        this.status.setContext(this);
    }

    public void handleUserLogin(UserClientDto dto) {
        status.handleUserLogin(dto);
    }

}
