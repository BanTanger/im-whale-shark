package com.bantanger.design.bridge.bridge.function;

import com.bantanger.design.bridge.bridge.abst.factory.RegisterLoginComponentFactory;
import com.bantanger.design.bridge.respository.UserRepository;
import com.bantanger.design.bridge.respository.dao.UserEntity;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:07
 */
@Component
public class RegisterLoginByDefault extends AbstractRegisterLoginFunc {

    @Resource
    private UserRepository userRepository;

    @PostConstruct
    private void initFuncMap() {
        RegisterLoginComponentFactory.funcMap.put("Default", this);
    }


    @Override
    public String login(String username, String password) {
        return super.commonLogin(username, password, userRepository);
    }

    @Override
    public String register(UserEntity userEntity) {
        return super.commonRegister(userEntity, userRepository);
    }

    @Override
    public boolean checkUserExists(String userName) {
        return super.commonCheckUserExists(userName, userRepository);
    }


}
