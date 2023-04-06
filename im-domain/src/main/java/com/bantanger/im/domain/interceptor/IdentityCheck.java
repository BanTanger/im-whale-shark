package com.bantanger.im.domain.interceptor;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.codec.utils.SigAPI;
import com.bantanger.im.common.BaseErrorCode;
import com.bantanger.im.common.ResponseVO;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.enums.error.GateWayErrorCode;
import com.bantanger.im.common.enums.user.UserTypeEnum;
import com.bantanger.im.common.exception.ApplicationExceptionEnum;
import com.bantanger.im.domain.user.dao.ImUserDataEntity;
import com.bantanger.im.domain.user.service.ImUserService;
import com.bantanger.im.service.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author BanTanger 半糖
 * @Date 2023/4/2 22:14
 */
@Slf4j
@Component
public class IdentityCheck {

    @Resource
    ImUserService userService;

    // TODO 后期需要将配置文件升级到数据库表进行持久化
    @Resource
    AppConfig appConfig;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    public ApplicationExceptionEnum checkUserSig(String identifier, String appId, String userSig) {
        // 10001:userSign:bantangereJyrVgrxCdZLrSjILEpVsjI0gAIdsHBiQYFnCkTUUAdJYUhmLkixmYWBiYm5sSVULjMlNa8kMy0ztUjJSikpMa8kMS8dyIZIFmemA0XziiJzQs0tvVLK-IPLjE1K9XOTMs3KyiL9w0qTwkMCsg1CLI2Kg-wiqyIibZVqAQHCL7I_
        String cacheUserSig = stringRedisTemplate.opsForValue().get(
                appId + Constants.RedisConstants.UserSign + identifier + userSig);
        if (!StringUtils.isBlank(cacheUserSig) &&
                Long.parseLong(cacheUserSig) > System.currentTimeMillis() / 1000) {
            this.setIsAdmin(identifier, Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        // 获取当前用户的密钥
        String privateKey = appConfig.getPrivateKey();

        // TODO 这一段逻辑需要更改，服务端生成密钥提供给客户端，而不是直接嵌套在这
        // 根据 appId + 密钥创建 sigApi(加密 token)
        SigAPI sigAPI = new SigAPI(Long.parseLong(appId), privateKey);

        // 调用 sigApi 对 userSig 解密
        JSONObject jsonObject = sigAPI.decodeUserSig(userSig);

        //取出解密后的 appid 和 操作人 和 过期时间做匹配，不通过则提示错误
        Long expireTime = 0L;
        Long expireSec = 0L;
        Long time = 0L;
        String decoderAppId = "";
        String decoderIdentifier = "";

        try {
            decoderAppId = jsonObject.getString("TLS.appId");
            decoderIdentifier = jsonObject.getString("TLS.identifier");
            String expireStr = jsonObject.get("TLS.expire").toString();
            String expireTimeStr = jsonObject.get("TLS.expireTime").toString();
            time = Long.valueOf(expireTimeStr);
            expireSec = Long.valueOf(expireStr);
            expireTime = Long.valueOf(expireTimeStr) + expireSec;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("checkUserSig-error:{}", e.getMessage());
        }

        if (!decoderIdentifier.equals(identifier)) {
            return GateWayErrorCode.USERSIGN_OPERATE_NOT_MATE;
        }

        if (!decoderAppId.equals(appId)) {
            return GateWayErrorCode.USERSIGN_IS_ERROR;
        }

        if (expireSec == 0L) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        if (expireTime < System.currentTimeMillis() / 1000) {
            return GateWayErrorCode.USERSIGN_IS_EXPIRED;
        }

        //appid + "xxx" + userId + sign
        String genSig = sigAPI.genUserSig(identifier, expireSec, time, null);
        if (genSig.toLowerCase().equals(userSig.toLowerCase())) {
            String key = appId + Constants.RedisConstants.UserSign + identifier + userSig;

            Long etime = expireTime - System.currentTimeMillis() / 1000;
            stringRedisTemplate.opsForValue().set(key, expireTime.toString(), etime, TimeUnit.SECONDS);
            this.setIsAdmin(identifier, Integer.valueOf(appId));
            return BaseErrorCode.SUCCESS;
        }

        return GateWayErrorCode.USERSIGN_IS_ERROR;
    }

    /**
     * 根据appid,identifier判断是否App管理员,并设置到RequestHolder
     * @param identifier
     * @param appId
     * @return
     */
    public void setIsAdmin(String identifier, Integer appId) {
        //去DB或Redis中查找, 后面写
        ResponseVO<ImUserDataEntity> singleUserInfo = userService.getSingleUserInfo(identifier, appId);
        if(singleUserInfo.isOk()){
            RequestHolder.set(singleUserInfo.getData().getUserType() == UserTypeEnum.APP_ADMIN.getCode());
        }else{
            RequestHolder.set(false);
        }
    }

}
