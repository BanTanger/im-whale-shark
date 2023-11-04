package com.bantanger.design.bridge.bridge.abst;

import com.bantanger.design.bridge.bridge.function.RegisterLoginFunc;
import com.bantanger.design.bridge.respository.dao.UserEntity;

import javax.servlet.http.HttpServletRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:57
 */
public class RegisterLoginComponent extends AbstractRegisterLoginComponent {

    public RegisterLoginComponent(RegisterLoginFunc funcInterface) {
        super(funcInterface);
    }

    @Override
    public String login(String username, String password) {
        return funcInterface.login(username, password);
    }

    @Override
    public String register(UserEntity userEntity) {
        return funcInterface.register(userEntity);
    }

    @Override
    public boolean checkUserExists(String username) {
        return funcInterface.checkUserExists(username);
    }

    @Override
    public String login3rd(HttpServletRequest request) {
        return funcInterface.login3rd(request);
    }

}
