package com.bantanger.design.ordinary.service;

import com.bantanger.design.ordinary.respository.UserRepository;
import com.bantanger.design.ordinary.respository.dao.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:45
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public String login(String username, String password) {
        UserEntity user = userRepository.findAccountByUserNameAndPassword(username, password);
        if (user == null) {
            log.warn("账号密码错误, 您输入的账号为 {} ", username);
            return "Login Fail";
        }
        log.info("用户 {} 登录成功", username);
        return "Login Success, username: " + username;
    }

    @Override
    public String register(UserEntity userEntity) {
        if (checkUserExists(userEntity.getUserName())) {
            log.info("{} 用户已存在", userEntity.getUserName());
            throw new RuntimeException("用户已存在");
        }
        userEntity.setCreateTime(new Date());
        int row = userRepository.createAccount(userEntity);
        if (row <= 0) {
            log.error("创建用户失败");
        }
        log.info("用户 {} 创建成功", userEntity.getUserName());
        return "Register Success, username: " + userEntity.getUserName();
    }

    @Override
    public String loginByGithub(String code, String state) {
        log.info("使用 github 登录...");
        // 逻辑写在 adapter、bridge, 这里就不写了
        return null;
    }

    @Override
    public String loginByWechat() {
        return null;
    }

    @Override
    public String loginByQQ() {
        return null;
    }

    public boolean checkUserExists(String username) {
        UserEntity user = userRepository.findAccountByUserName(username);
        if (user == null) {
            log.warn("不存在 {} 用户，请注册", username);
            log.info("重定向到 index 页面进行注册逻辑 ...");
            return false;
        }
        log.info("用户 {} 存在", username);
        return true;
    }
}

