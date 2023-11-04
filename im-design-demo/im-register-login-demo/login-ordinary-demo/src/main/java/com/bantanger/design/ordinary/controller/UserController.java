package com.bantanger.design.ordinary.controller;

import com.bantanger.design.ordinary.respository.dao.UserEntity;
import com.bantanger.design.ordinary.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:23
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/ordinary/v1")
public class UserController {

    private final UserService userServiceImpl;

    @PostMapping("/login")
    public String login(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            log.error("账号或密码不能为空");
            return "error 500";
        }
        log.info("密码加盐处理...");
        return userServiceImpl.login(account, password);
    }

    @PostMapping("/register")
    public String register(@RequestBody UserEntity userEntity) {
        return userServiceImpl.register(userEntity);
    }

}

