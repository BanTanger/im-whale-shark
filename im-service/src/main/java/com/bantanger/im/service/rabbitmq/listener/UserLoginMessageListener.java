package com.bantanger.im.service.rabbitmq.listener;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.common.constant.Constants;
import com.bantanger.im.common.model.UserClientDto;
import com.bantanger.im.service.redis.RedisManager;
import com.bantanger.im.service.strategy.login.factory.LoginStatusFactory;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.listener.MessageListener;

/**
 * 多端同步：
 * 单平台登录	:仅可有 1 种平台在线:	Android、iPhone、iPad、Windows、Mac、Web仅可有1种平台在线；
 * 双平台登录	:移动或桌面平台可有 1 种平台在线 + Web 可同时在线:	Android、iPhone、iPad、Windows、Mac可有1端在线；Web可同时在线
 * 三平台登录	:移动平台可有 1 种平台在线 + 桌面平台可以有 1 种平台在线 + Web 可同时在线:	Android、iPhone、iPad可有1种平台在线；Windows、Mac可有1种平台在线；Web可同时在线；
 * 多平台登录	:不同平台均可同时在线:	Android、iPhone、iPad、Windows、Mac、Web可全平台同时在线；
 *
 * @author BanTanger 半糖
 * @Date 2023/3/26 16:26
 */
@Slf4j
public class UserLoginMessageListener {

    private Integer loginModel;

    public UserLoginMessageListener(Integer loginModel) {
        this.loginModel = loginModel;
    }

    public void listenerUserLogin() {
        // 监听者监听 UserLoginChannel 队列
        RTopic topic = RedisManager.getRedissonClient().getTopic(Constants.RedisConstants.UserLoginChannel);
        topic.addListener(String.class, (CharSequence charSequence, String msg) -> {
            log.info("收到用户上线通知 {}", msg);
            UserClientDto dto = JSONObject.parseObject(msg, UserClientDto.class);
            LoginStatusFactory loginStatusFactory = new LoginStatusFactory();
            loginStatusFactory.chooseLoginStatus(loginModel);
            loginStatusFactory.handleUserLogin(dto);
        });
    }
}
