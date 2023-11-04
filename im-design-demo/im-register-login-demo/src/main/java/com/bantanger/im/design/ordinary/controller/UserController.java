package com.bantanger.im.design.ordinary.controller;

import com.bantanger.im.design.adapter.service.Login3rdAdapter;
import com.bantanger.im.design.common.respository.dao.UserEntity;
import com.bantanger.im.design.common.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:23
 */
@Slf4j
@RestController
@RequiredArgsConstructor
// @RequestMapping("/ordinary/v1")
public class UserController {

    // private final UserService userService;
    private final Login3rdAdapter login3rdAdapter;

    @PostMapping("/login")
    public String login(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            log.error("账号或密码不能为空");
            return "error 500";
        }
        log.info("密码加盐处理...");
        return login3rdAdapter.login(account, password);
    }

    @PostMapping("/register")
    public String register(@RequestBody UserEntity userEntity) {
        return login3rdAdapter.register(userEntity);
    }

    @GetMapping("/github")
    public String login3rdByGithub(String code, String state) throws IOException {
        return login3rdAdapter.loginByGithub(code, state);
    }

}

