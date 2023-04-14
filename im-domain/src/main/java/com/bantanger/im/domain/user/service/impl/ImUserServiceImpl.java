package com.bantanger.im.domain.user.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.pack.user.UserModifyPack;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.command.UserEventCommand;
import com.bantanger.im.domain.group.service.ImGroupService;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.domain.user.dao.mapper.ImUserDataMapper;
import com.bantanger.im.domain.user.model.req.*;
import com.bantanger.im.domain.user.model.resp.GetUserInfoResp;
import com.bantanger.im.domain.user.model.resp.ImportUserResp;
import com.bantanger.im.domain.user.service.ImUserService;
import com.bantanger.im.service.callback.CallbackService;
import com.bantanger.im.service.config.AppConfig;
import com.bantanger.im.service.sendmsg.MessageProducer;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.enums.friend.DelFlagEnum;
import com.bantanger.im.common.enums.user.UserErrorCode;
import com.bantanger.im.common.exception.ApplicationException;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/16
 */
@Service
public class ImUserServiceImpl implements ImUserService {

    @Resource
    ImUserDataMapper imUserDataMapper;

    @Resource
    ImGroupService imGroupService;

    @Resource
    AppConfig appConfig;

    @Resource
    CallbackService callbackService;

    @Resource
    MessageProducer messageProducer;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public ResponseVO importUser(ImportUserReq req) {

        if (req.getUserData().size() > 100) {
            return ResponseVO.errorResponse(UserErrorCode.IMPORT_SIZE_BEYOND);
        }

        ImportUserResp resp = new ImportUserResp();
        List<String> successId = new ArrayList<>();
        List<String> errorId = new ArrayList<>();

        for (ImUserDataEntity data :
                req.getUserData()) {
            try {
                data.setAppId(req.getAppId());
                int insert = imUserDataMapper.insert(data);
                if (insert == 1) {
                    successId.add(data.getUserId());
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorId.add(data.getUserId());
            }
        }

        resp.setErrorId(errorId);
        resp.setSuccessId(successId);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req) {
        QueryWrapper<ImUserDataEntity> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("app_id", req.getAppId());
        queryWrapper.in("user_id", req.getUserIds());
        queryWrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        List<ImUserDataEntity> userDataEntities = imUserDataMapper.selectList(queryWrapper);
        Map<String, ImUserDataEntity> map = new HashMap<>();

        for (ImUserDataEntity data : userDataEntities) {
            map.put(data.getUserId(), data);
        }

        List<String> failUser = new ArrayList<>();
        for (String uid : req.getUserIds()) {
            if (!map.containsKey(uid)) {
                failUser.add(uid);
            }
        }

        GetUserInfoResp resp = new GetUserInfoResp();
        resp.setUserDataItem(userDataEntities);
        resp.setFailUser(failUser);
        return ResponseVO.successResponse(resp);
    }

    @Override
    public ResponseVO<ImUserDataEntity> getSingleUserInfo(String userId, Integer appId) {
        QueryWrapper wrapper = new QueryWrapper<>();
        wrapper.eq("app_id", appId);
        wrapper.eq("user_id", userId);
        wrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());

        ImUserDataEntity imUserDataEntity = imUserDataMapper.selectOne(wrapper);
        if (imUserDataEntity == null) {
            return ResponseVO.errorResponse(UserErrorCode.USER_IS_NOT_EXIST);
        }

        return ResponseVO.successResponse(imUserDataEntity);
    }

    @Override
    public ResponseVO deleteUser(DeleteUserReq req) {
        ImUserDataEntity entity = new ImUserDataEntity();
        entity.setDelFlag(DelFlagEnum.DELETE.getCode());

        List<String> errorId = new ArrayList();
        List<String> successId = new ArrayList();

        for (String userId :
                req.getUserId()) {
            QueryWrapper wrapper = new QueryWrapper();
            wrapper.eq("app_id", req.getAppId());
            wrapper.eq("user_id", userId);
            wrapper.eq("del_flag", DelFlagEnum.NORMAL.getCode());
            int update = 0;

            try {
                update = imUserDataMapper.update(entity, wrapper);
                if (update > 0) {
                    successId.add(userId);
                } else {
                    errorId.add(userId);
                }
            } catch (Exception e) {
                errorId.add(userId);
            }
        }

        ImportUserResp resp = new ImportUserResp();
        resp.setSuccessId(successId);
        resp.setErrorId(errorId);
        return ResponseVO.successResponse(resp);
    }

    @Override
    @Transactional
    public ResponseVO modifyUserInfo(ModifyUserInfoReq req) {
        QueryWrapper query = new QueryWrapper<>();
        query.eq("app_id", req.getAppId());
        query.eq("user_id", req.getUserId());
        query.eq("del_flag", DelFlagEnum.NORMAL.getCode());
        ImUserDataEntity user = imUserDataMapper.selectOne(query);
        if (user == null) {
            throw new ApplicationException(UserErrorCode.USER_IS_NOT_EXIST);
        }

        ImUserDataEntity update = new ImUserDataEntity();
        update.setUserId(req.getUserId());
        update.setNickName(req.getNickName());
        update.setLocation(req.getLocation());
        update.setBirthDay(req.getBirthDay());
        update.setPassword(req.getPassword());
        update.setPhoto(req.getPhoto());
        update.setUserSex(req.getUserSex());
        update.setSelfSignature(req.getSelfSignature());
        update.setFriendAllowType(req.getFriendAllowType());
        update.setAppId(req.getAppId());
        update.setExtra(req.getExtra());

        // TODO ?
        update.setAppId(null);
        update.setUserId(null);
        int update1 = imUserDataMapper.update(update, query);
        if (update1 == 1) {
            // 在回调开始前，先发送 TCP 通知，保证数据同步
            UserModifyPack pack = new UserModifyPack();
            pack.setUserId(req.getUserId());
            pack.setNickName(req.getNickName());
            pack.setPassword(req.getPassword());
            pack.setPhoto(req.getPhoto());
            pack.setUserSex(req.getUserSex());
            pack.setSelfSignature(req.getSelfSignature());
            pack.setFriendAllowType(req.getFriendAllowType());

            messageProducer.sendMsgToUser(req.getUserId(), UserEventCommand.USER_MODIFY,
                    pack, req.getAppId(), req.getClientType(), req.getImei());

            // 若修改成功且开启修改用户信息的业务回调，则发起回调
            if (appConfig.isModifyUserAfterCallback()) {
                callbackService.afterCallback(req.getAppId(),
                        Constants.CallbackCommand.ModifyUserAfter,
                        JSONObject.toJSONString(req));
            }
            return ResponseVO.successResponse();
        }
        throw new ApplicationException(UserErrorCode.MODIFY_USER_ERROR);
    }

    @Override
    public ResponseVO login(LoginReq req) {
        // TODO 后期补充鉴权
        return ResponseVO.successResponse();
    }

    @Override
    public ResponseVO getUserSequence(GetUserSequenceReq req) {
        // 将 redis 的缓存信息存入
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(
                req.getAppId() + ":" + Constants.RedisConstants.SeqPrefix + ":" + req.getUserId());
        Long groupSeq = imGroupService.getUserGroupMaxSeq(req.getUserId(), req.getAppId());
        map.put(Constants.SeqConstants.GroupSeq, groupSeq);
        return ResponseVO.successResponse(map);
    }
}
