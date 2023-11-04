package com.bantanger.im.design.adapter.service;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.im.design.adapter.utils.HttpClientUtils;
import com.bantanger.im.design.common.respository.UserRepository;
import com.bantanger.im.design.common.respository.dao.UserEntity;
import com.bantanger.im.design.common.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 10:58
 */
@Slf4j
@Component
public class Login3rdAdapter extends UserService implements Login3rdTarget {

    @Value("${github.state:}")
    private String githubState;

    @Value("${github.token_url:}")
    private String githubTokenUrl;

    @Value("${github.user_url:}")
    private String githubUserUrl;

    @Value("${github.user_prefix:}")
    private String githubUserPrefix;

    public Login3rdAdapter(UserRepository userRepository) {
        super(userRepository);
    }

    @Override
    public String loginByGithub(String code, String state) {
        // github 回调该接口会携带 state, 防止跨站请求伪造攻击
        if (!githubState.equals(state)) {
            log.warn("github 回调地址 state 不一致, 期望: {}, 实际: {}", githubState, state);
            throw new UnsupportedOperationException("不是预期 state！");
        }
        // 请求 Github 平台获取 Token，并携带 code
        String tokenUrl = githubTokenUrl.concat(code);
        String tokenResponse = HttpClientUtils.execute2(tokenUrl, HttpMethod.GET);
        // 请求用户信息，携带 token
        String token = tokenResponse.substring(tokenResponse.indexOf("=") + 1);
        String userInfoResponse = HttpClientUtils.execute(githubUserUrl, token);

        // 获取用户信息，username 加上 GITHUB@ 前缀，密码与 username 一致，这里就不加密了
        String username = githubUserPrefix.concat(userInfoResponse);
        String password = username;

        return autoRegister3rdAndLogin(username, password);
    }

    private String autoRegister3rdAndLogin(String username, String password) {
        //如果第三方账号已经登录过，则直接登录
        if(super.checkUserExists(username)) {
            return super.login(username, password);
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(username);
        userEntity.setPassword(password);
        userEntity.setCreateTime(new Date());

        //如果第三方账号是第一次登录，先进行“自动注册”
        super.register(userEntity);
        //自动注册完成后，进行登录
        return super.login(username, password);
    }

    @Override
    public String loginByWechat() {
        return null;
    }

    @Override
    public String loginByQQ() {
        return null;
    }
}
