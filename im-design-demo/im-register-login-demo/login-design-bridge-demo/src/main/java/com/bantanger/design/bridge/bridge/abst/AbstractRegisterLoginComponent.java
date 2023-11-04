package com.bantanger.design.bridge.bridge.abst;

import com.bantanger.design.bridge.bridge.function.RegisterLoginFunc;
import com.bantanger.design.bridge.respository.dao.UserEntity;

import javax.servlet.http.HttpServletRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:27
 */
public abstract class AbstractRegisterLoginComponent {

    protected RegisterLoginFunc funcInterface;

    public AbstractRegisterLoginComponent(RegisterLoginFunc funcInterface) {
        this.funcInterface = funcInterface;
    }

    public abstract String login(String username, String password);
    public abstract String register(UserEntity userEntity);
    public abstract boolean checkUserExists(String username);
    public abstract String login3rd(HttpServletRequest request);

}
