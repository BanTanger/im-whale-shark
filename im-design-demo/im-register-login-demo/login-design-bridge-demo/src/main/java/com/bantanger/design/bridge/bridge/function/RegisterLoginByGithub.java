package com.bantanger.design.bridge.bridge.function;

import com.alibaba.fastjson.JSONObject;
import com.bantanger.design.bridge.bridge.abst.factory.RegisterLoginComponentFactory;
import com.bantanger.design.bridge.respository.UserRepository;
import com.bantanger.design.bridge.respository.dao.UserEntity;
import com.bantanger.design.bridge.utils.HttpClientUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:22
 */
@Slf4j
@Component
public class RegisterLoginByGithub extends AbstractRegisterLoginFunc implements RegisterLoginFunc {

    @Value("${github.state:}")
    private String githubState;

    @Value("${github.token_url:}")
    private String githubTokenUrl;

    @Value("${github.user_url:}")
    private String githubUserUrl;

    @Value("${github.user_prefix:}")
    private String githubUserPrefix;

    @Value("${github.authorize_uri:}")
    private String authorizeUri;

    @Resource
    private UserRepository userRepository;

    @PostConstruct
    private void initFuncMap() {
        RegisterLoginComponentFactory.funcMap.put("GITHUB", this);
        log.info("请求 Oauth uri 为 {}", authorizeUri);
    }

    @Override
    public String login3rd(HttpServletRequest request) {
        String code = request.getParameter("code");
        String state = request.getParameter("state");

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
        JSONObject userInfoResponse = HttpClientUtils.execute(githubUserUrl, token);

        // 获取用户信息，username 加上 GITHUB@ 前缀，密码与 username 一致，这里就不加密了
        String username = githubUserPrefix.concat(String.valueOf(userInfoResponse.get("login")));
        String password = username;

        return autoRegister3rdAndLogin(username, password);
    }

    private String autoRegister3rdAndLogin(String username, String password) {
        //如果第三方账号已经登录过，则直接登录
        if(super.commonCheckUserExists(username, userRepository)) {
            return super.commonLogin(username, password, userRepository);
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(username);
        userEntity.setPassword(password);
        userEntity.setCreateTime(new Date());

        //如果第三方账号是第一次登录，先进行“自动注册”
        super.commonRegister(userEntity, userRepository);
        //自动注册完成后，进行登录
        return super.commonLogin(username, password, userRepository);
    }



}
