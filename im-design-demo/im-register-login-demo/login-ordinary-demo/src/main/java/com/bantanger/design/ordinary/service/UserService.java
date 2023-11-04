package com.bantanger.design.ordinary.service;

import com.bantanger.design.ordinary.respository.dao.UserEntity;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 17:03
 */
public interface UserService {

    /**
     * 账号密码登录
     * @param username
     * @param password
     * @return
     */
    String login(String username, String password);

    /**
     * 用户填写表单注册逻辑
     * @param userEntity 用户填写表单构建的实体对象
     * @return
     */
    String register(UserEntity userEntity);

    /**
     * 第三方登录：github
     * @param code
     * @param state
     * @return
     */
    String loginByGithub(String code, String state);

    /**
     * 第三方登录：微信
     * @return
     */
    String loginByWechat();

    /**
     * 第三方登录：qq
     * @return
     */
    String loginByQQ();
}
