package com.bantanger.im.domain.friendship.controller;

import com.bantanger.im.common.model.SyncReq;
import com.bantanger.im.domain.friendship.model.req.*;
import com.bantanger.im.domain.friendship.model.req.friend.*;
import com.bantanger.im.domain.friendship.service.ImFriendService;
import com.bantanger.im.common.ResponseVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("v1/friendship")
public class ImFriendShipController {

    @Resource
    ImFriendService imFriendShipService;

    /**
     * importFriendShip
     * http://localhost:8000/v1/friendship/importFriendShip?appId=10001
     * @param req
     * {
     *     "fromId":"lld2",
     *     "friendItem":[
     *         {
     *             "remark":"备注",
     *             "toId":"123456"
     *         }
     *     ]
     * }
     * @return
     * 第一次插入(成功)：{
     *     "code": 200,
     *     "msg": "success",
     *     "data": { "successId": ["123456"],"errorId": []},
     *     "ok": true
     * }
     * 第二次插入(失败)：{
     *     "code": 200,
     *     "msg": "success",
     *     "data": { "successId": [],"errorId": ["123456"]},
     *     "ok": true
     * }
     */
    @PostMapping("/importFriendShip")
    public ResponseVO importFriendShip(@RequestBody @Validated ImportFriendShipReq req){
        return imFriendShipService.importFriendShip(req);
    }

    /**
     * 添加好友逻辑
     * 分两种情况，如果对方设置好友验证，会走发送好友申请逻辑；否则直接添加双方强好友关系
     * 数据库内置数据 bantanger 开启好友验证，其余 10001 ~ 10009 均未开启
     * <br/> 情况一：添加未开启好友验证的用户
     * <pre>
     * curl -X POST \
     *   http://localhost:18000/v1/friendship/addFriend \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "fromId": "bantanger",
     *     "toItem": {
     *       "toId": "10004",
     *       "remark": "你好，我叫 bantanger",
     *       "addSource": "个人搜索",
     *       "addWording": "你好，我叫 bantanger"
     *     },
     *     "appId": 10001,
     *     "operater": "bantanger",
     *     "clientType": 2,
     *     "imei": "200"
     *   }'
     * </pre>
     * <br/> 情况二：添加开启好友验证的用户
     * <pre>
     * curl -X POST \
     *   http://localhost:18000/v1/friendship/addFriend \
     *   -H 'Content-Type: application/json' \
     *   -d '{
     *     "fromId": "10005",
     *     "toItem": {
     *       "toId": "bantanger",
     *       "remark": "你好，我叫 10005",
     *       "addSource": "二维码",
     *       "addWording": "你好，我叫 10005"
     *     },
     *     "appId": 10001,
     *     "operater": "10005",
     *     "clientType": 2,
     *     "imei": "200"
     *   }'
     * </pre>
     * @param req
     * @return
     */
    @PostMapping("/addFriend")
    public ResponseVO addFriend(@RequestBody @Validated AddFriendReq req){
        return imFriendShipService.addFriend(req);
    }

    @PostMapping("/updateFriend")
    public ResponseVO updateFriend(@RequestBody @Validated UpdateFriendReq req){
        return imFriendShipService.updateFriend(req);
    }

    @PostMapping("/deleteFriend")
    public ResponseVO deleteFriend(@RequestBody @Validated DeleteFriendReq req){
        return imFriendShipService.deleteFriend(req);
    }

    @PostMapping("/deleteAllFriend")
    public ResponseVO deleteAllFriend(@RequestBody @Validated DeleteFriendReq req){
        return imFriendShipService.deleteAllFriend(req);
    }

    @PostMapping("/getAllFriendShip")
    public ResponseVO getAllFriendShip(@RequestBody @Validated GetAllFriendShipReq req){
        return imFriendShipService.getAllFriendShip(req);
    }

    @PostMapping("/getRelation")
    public ResponseVO getRelation(@RequestBody @Validated GetRelationReq req){
        return imFriendShipService.getRelation(req);
    }

    @PostMapping("/checkFriend")
    public ResponseVO checkFriend(@RequestBody @Validated CheckFriendShipReq req){
        return imFriendShipService.checkFriendship(req);
    }

    @PostMapping("/addBlack")
    public ResponseVO addBlack(@RequestBody @Validated AddFriendShipBlackReq req){
        return imFriendShipService.addBlack(req);
    }

    @PostMapping("/deleteBlack")
    public ResponseVO deleteBlack(@RequestBody @Validated DeleteBlackReq req){
        return imFriendShipService.deleteBlack(req);
    }

    @PostMapping("/checkBlck")
    public ResponseVO checkBlck(@RequestBody @Validated CheckFriendShipReq req){
        return imFriendShipService.checkBlck(req);
    }

    /**
     * 同步好友列表
     * @param req
     * @return
     */
    @PostMapping("/syncFriendShipList")
    public ResponseVO syncFriendShipList(@RequestBody @Validated SyncReq req){
        return imFriendShipService.syncFriendShipList(req);
    }

}
