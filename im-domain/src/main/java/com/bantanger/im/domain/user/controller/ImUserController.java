package com.bantanger.im.domain.user.controller;

import com.bantanger.im.common.enums.device.ClientType;
import com.bantanger.im.domain.user.model.req.DeleteUserReq;
import com.bantanger.im.domain.user.model.req.GetUserSequenceReq;
import com.bantanger.im.domain.user.model.req.ImportUserReq;
import com.bantanger.im.domain.user.model.req.LoginReq;
import com.bantanger.im.domain.user.service.ImUserService;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.service.route.RouteHandler;
import com.bantanger.im.service.route.RouteInfo;
import com.bantanger.im.service.utils.RouteInfoParseUtil;
import com.bantanger.im.service.zookeeper.ZkManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16 20:07
 */
@RestController
@RequestMapping("v1/user")
public class ImUserController {

    @Resource
    ImUserService imUserService;

    @Resource
    RouteHandler routeHandler;

    @Resource
    ZkManager zkManager;

    @RequestMapping("importUser")
    public ResponseVO importUser(@RequestBody ImportUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.importUser(req);
    }

    @RequestMapping("/deleteUser")
    public ResponseVO deleteUser(@RequestBody @Validated DeleteUserReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.deleteUser(req);
    }

    @RequestMapping("/login")
    public ResponseVO login(@RequestBody @Validated LoginReq req, Integer appId) {
        req.setAppId(appId);
        ResponseVO login = imUserService.login(req);
        if (login.isOk()) {
            // 从 Zk 获取 im 地址，返回给 sdk
            List<String> allNode = new ArrayList<>();
            if (ClientType.WEB.getCode().equals(req.getClientType())) {
                allNode = zkManager.getAllWebNode();
            } else {
                allNode = zkManager.getAllTcpNode();
            }
            // ip:port
            String s = routeHandler.routeServer(allNode, req.getUserId());
            RouteInfo parse = RouteInfoParseUtil.parse(s);
            return ResponseVO.successResponse(parse);
        }
        return ResponseVO.errorResponse();
    }

    /**
     * 客户端向服务端请求该用户各接口需要拉取的数量
     * @param req
     * @param appId
     * @return
     */
    @RequestMapping("/getUserSequence")
    public ResponseVO getUserSequence(@RequestBody @Validated GetUserSequenceReq req, Integer appId) {
        req.setAppId(appId);
        return imUserService.getUserSequence(req);
    }

}
