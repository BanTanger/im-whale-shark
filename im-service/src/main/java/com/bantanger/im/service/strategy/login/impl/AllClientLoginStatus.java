package com.bantanger.im.service.strategy.login.impl;

import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.strategy.login.LoginStatus;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:19
 */
public class AllClientLoginStatus extends LoginStatus {
    @Override
    public void switchStatus(LoginStatus status) {
        context.setStatus(status);
    }

    @Override
    public void handleUserLogin(UserClientDto dto) {
        // 放权，允许多设备登录，同端之间也不做逻辑处理
    }
}
