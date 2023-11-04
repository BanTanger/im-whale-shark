package com.bantanger.design.bridge.service;

import com.bantanger.design.bridge.bridge.abst.AbstractRegisterLoginComponent;
import com.bantanger.design.bridge.bridge.abst.factory.RegisterLoginComponentFactory;
import com.bantanger.design.bridge.respository.dao.UserEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 22:17
 */
@Service
public class UserBridgeService {

    public String login(String account, String password) {
        AbstractRegisterLoginComponent component =
                RegisterLoginComponentFactory.getComponent("Default");
        return component.login(account, password);
    }

    public String register(UserEntity userEntity) {
        AbstractRegisterLoginComponent component =
                RegisterLoginComponentFactory.getComponent("Default");
        return component.register(userEntity);
    }

    public String login3rd(HttpServletRequest request, String type) {
        AbstractRegisterLoginComponent component =
                RegisterLoginComponentFactory.getComponent(type);
        return component.login3rd(request);
    }
}
