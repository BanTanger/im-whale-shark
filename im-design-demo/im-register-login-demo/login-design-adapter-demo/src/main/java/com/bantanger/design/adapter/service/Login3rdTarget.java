package com.bantanger.design.adapter.service;

/**
 * @author BanTanger 半糖
 * @Date 2023/11/4 10:59
 */
public interface Login3rdTarget {

    /**
     * github oauth2 方式登录
     * @param code
     * @param state
     * @return
     */
    String loginByGithub(String code, String state);

    /**
     * 微信方式登录
     * @return
     */
    String loginByWechat();

    /**
     * QQ 方式登录（还可以继续分：扫码，oauth2 登录方式）
     * @return
     */
    String loginByQQ();
}
