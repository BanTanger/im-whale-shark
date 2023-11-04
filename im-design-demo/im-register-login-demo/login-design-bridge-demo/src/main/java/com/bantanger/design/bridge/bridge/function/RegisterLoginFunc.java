package com.bantanger.design.bridge.bridge.function;

import com.bantanger.design.bridge.respository.dao.UserEntity;

import javax.servlet.http.HttpServletRequest;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 21:04
 */
public interface RegisterLoginFunc {

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
     * 第三方登录
     * @param request
     * @return
     */
    String login3rd(HttpServletRequest request);

    /**
     * 检查用户是否存在
     * @param userName
     * @return
     */
    boolean checkUserExists(String userName);

}
