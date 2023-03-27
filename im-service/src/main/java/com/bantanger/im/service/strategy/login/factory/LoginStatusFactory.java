package com.bantanger.im.service.strategy.login.factory;

import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.strategy.login.LoginContext;
import com.bantanger.im.service.strategy.login.LoginStatus;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:56
 */
public class LoginStatusFactory extends LoginStatusFactoryConfig {

    private LoginContext ctx = new LoginContext();

    /**
     * 上下文存储、路由用户所选择的端同步类型
     * @param status
     */
    public void chooseLoginStatus(Integer status) {
        LoginStatus loginStatus = LoginStatusMap.get(status);
        ctx.setStatus(loginStatus);
    }

    /**
     * 处理用户所选择端同步类型，判断是否需要下线 channel 旧信息
     * @param dto
     */
    public void handleUserLogin(UserClientDto dto) {
        ctx.handleUserLogin(dto);
    }

}
