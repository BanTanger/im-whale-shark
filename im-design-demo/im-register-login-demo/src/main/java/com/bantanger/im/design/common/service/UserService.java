package com.bantanger.im.design.common.service;

import com.bantanger.im.design.common.respository.UserRepository;
import com.bantanger.im.design.common.respository.dao.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:45
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public String login(String username, String password) {
        UserEntity user = userRepository.findAccountByUserNameAndPassword(username, password);
        if (user == null) {
            log.warn("账号密码错误, 您输入的账号为 {} ", username);
            return "Login Fail";
        }
        log.info("用户 {} 登录成功", username);
        return "Login Success";
    }

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
        return "Register Success";
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

