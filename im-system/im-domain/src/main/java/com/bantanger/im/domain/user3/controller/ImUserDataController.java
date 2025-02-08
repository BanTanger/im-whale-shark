package com.bantanger.im.domain.user3.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.user3.model.req.GetUserInfoReq;
import com.bantanger.im.domain.user3.model.req.ModifyUserInfoReq;
import com.bantanger.im.domain.user3.model.req.UserId;
import com.bantanger.im.domain.user3.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@Slf4j
@RestController
@RequestMapping("v1/user/data")
public class ImUserDataController {

    @Resource
    ImUserService imUserService;

    @PostMapping("/getUserInfo")
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq req, Integer appId){//@Validated
        req.setAppId(appId);
        return imUserService.getUserInfo(req);
    }

    @PostMapping("/getSingleUserInfo")
    public ResponseVO getSingleUserInfo(@RequestBody @Validated UserId req, Integer appId){
        req.setAppId(appId);
        return imUserService.getSingleUserInfo(req.getUserId(),req.getAppId());
    }

    @PostMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated ModifyUserInfoReq req, Integer appId){
        req.setAppId(appId);
        return imUserService.modifyUserInfo(req);
    }
}
