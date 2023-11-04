package com.bantanger.design.bridge.bridge.function;

import com.bantanger.design.bridge.respository.UserRepository;
import com.bantanger.design.bridge.respository.dao.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:08
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractRegisterLoginFunc implements RegisterLoginFunc {

    /**
     * 抽象方法无法通过 @Autowired 注入 Bean
     *
     * 要么通过构造函数延迟到子类注入，要么使用 Setter 注入
     *
     * 这里使用第三种方式，直接注入，因为考虑到方便
     */

    protected String commonLogin(String account, String password, UserRepository userRepository) {
        UserEntity user = userRepository.findAccountByUserNameAndPassword(account, password);
        if (user == null) {
            log.warn("账号密码错误, 您输入的账号为 {} ", account);
            return "Login Fail";
        }
        log.info("用户 {} 登录成功", account);
        return "Login Success, username: " + account;
    }

    protected String commonRegister(UserEntity userEntity, UserRepository userRepository) {
        if (commonCheckUserExists(userEntity.getUserName(), userRepository)) {
            log.info("{} 用户已存在", userEntity.getUserName());
            throw new RuntimeException("用户已存在");
        }
        userEntity.setCreateTime(new Date());
        int row = userRepository.createAccount(userEntity);
        if (row <= 0) {
            log.error("创建用户失败");
        }
        log.info("用户 {} 创建成功", userEntity.getUserName());
        return "Register Success, username: " + userEntity.getUserName();
    }

    public boolean commonCheckUserExists(String username, UserRepository userRepository) {
        UserEntity user = userRepository.findAccountByUserName(username);
        if (user == null) {
            log.warn("不存在 {} 用户，请注册", username);
            log.info("重定向到 index 页面进行注册逻辑 ...");
            return false;
        }
        log.info("用户 {} 存在", username);
        return true;
    }

    @Override
    public String login(String username, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String login3rd(HttpServletRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String register(UserEntity userEntity){
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkUserExists(String username){
        throw new UnsupportedOperationException();
    }

}
