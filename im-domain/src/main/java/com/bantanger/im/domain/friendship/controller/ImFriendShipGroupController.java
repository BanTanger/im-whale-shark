package com.bantanger.im.domain.friendship.controller;

import com.bantanger.im.domain.friendship.service.ImFriendShipGroupMemberService;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.domain.friendship.model.req.group.member.AddFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.model.req.group.AddFriendShipGroupReq;
import com.bantanger.im.domain.friendship.model.req.group.member.DeleteFriendShipGroupMemberReq;
import com.bantanger.im.domain.friendship.model.req.group.DeleteFriendShipGroupReq;
import com.bantanger.im.domain.friendship.service.ImFriendShipGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/25 15:16
 */
@RestController
@RequestMapping("v1/friendship/group")
public class ImFriendShipGroupController {

    @Resource
    ImFriendShipGroupService imFriendShipGroupService;

    @Resource
    ImFriendShipGroupMemberService imFriendShipGroupMemberService;

    @RequestMapping("/add")
    public ResponseVO add(@RequestBody @Validated AddFriendShipGroupReq req)  {
        return imFriendShipGroupService.addGroup(req);
    }

    @RequestMapping("/del")
    public ResponseVO del(@RequestBody @Validated DeleteFriendShipGroupReq req)  {
        return imFriendShipGroupService.deleteGroup(req);
    }

    @RequestMapping("/member/add")
    public ResponseVO memberAdd(@RequestBody @Validated AddFriendShipGroupMemberReq req)  {
        return imFriendShipGroupMemberService.addGroupMember(req);
    }

    @RequestMapping("/member/del")
    public ResponseVO memberdel(@RequestBody @Validated DeleteFriendShipGroupMemberReq req)  {
        return imFriendShipGroupMemberService.delGroupMember(req);
    }


}
