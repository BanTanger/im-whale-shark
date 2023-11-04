package com.bantanger.design.bridge.controller;

import com.bantanger.design.bridge.respository.dao.UserEntity;
import com.bantanger.design.bridge.service.UserBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/3 23:23
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bridge/v1")
public class UserController {

    private final UserBridgeService userBridgeService;

    @PostMapping("/login")
    public String login(String account, String password) {
        if (StringUtils.isBlank(account) || StringUtils.isBlank(password)) {
            log.error("账号或密码不能为空");
            return "error 500";
        }
        log.info("密码加盐处理...");
        return userBridgeService.login(account, password);
    }

    @PostMapping("/register")
    public String register(@RequestBody UserEntity userEntity) {
        return userBridgeService.register(userEntity);
    }

    @GetMapping("/github")
    public String login3rdByGithub(HttpServletRequest request) {
        return userBridgeService.login3rd(request, "GITHUB");
    }

}

