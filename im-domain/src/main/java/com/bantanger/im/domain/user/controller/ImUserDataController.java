package com.bantanger.im.domain.user.controller;

import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.user.model.req.GetUserInfoReq;
import com.bantanger.im.domain.user.model.req.ModifyUserInfoReq;
import com.bantanger.im.domain.user.model.req.UserId;
import com.bantanger.im.domain.user.service.ImUserService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

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
    public ResponseVO getUserInfo(@RequestBody GetUserInfoReq req){
        return imUserService.getUserInfo(req);
    }

    @PostMapping("/getSingleUserInfo")
    public ResponseVO getSingleUserInfo(@RequestBody @Validated UserId req){
        return imUserService.getSingleUserInfo(req.getUserId(),req.getAppId());
    }

    @PostMapping("/modifyUserInfo")
    public ResponseVO modifyUserInfo(@RequestBody @Validated ModifyUserInfoReq req){
        return imUserService.modifyUserInfo(req);
    }
}
