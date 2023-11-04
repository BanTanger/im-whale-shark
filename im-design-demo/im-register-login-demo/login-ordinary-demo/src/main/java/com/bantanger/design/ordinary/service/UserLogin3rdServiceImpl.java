package com.bantanger.design.ordinary.service;

import com.bantanger.design.ordinary.respository.dao.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 17:09
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserLogin3rdServiceImpl implements UserService {

    /**
     * 可以看到我们这个类其实是想实现第三方接口相关的逻辑，
     * 但由于继承了 UserService 接口，不得不实现它的所有方法
     * 有没有什么更好的方式呢，往接口里加 default 固然是种选择，但这就有点违背接口的本意
     *
     * 接口的本意是子类实现它的所有方法，若接口新增方法，所有子类没有实现就会产生编译错误爆红
     * 这样的意义是为了让我们知道到底有那些子类还没有实现方法
     */

    @Override
    public String login(String username, String password) {
        return null;
    }

    @Override
    public String register(UserEntity userEntity) {
        return null;
    }

    @Override
    public String loginByGithub(String code, String state) {
        log.info("使用 github 登录...");
        // 逻辑写在 adapter、bridge, 这里就不写了
        return "Success！！！";
    }

    @Override
    public String loginByWechat() {
        log.info("使用微信扫码登录...");
        return "Success！！！";
    }

    @Override
    public String loginByQQ() {
        log.info("使用 QQ 登录...");
        return "Success！！！";
    }

}
