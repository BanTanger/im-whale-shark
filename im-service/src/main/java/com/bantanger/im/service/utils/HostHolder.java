package com.bantanger.im.service.utils;

import com.bantanger.im.common.model.UserClientDto;

/**
 * ThreadLocal 存储用户信息
 * @author BanTanger 半糖
 * @Date 2023/3/25 16:39
 */
public class HostHolder {

    /**
     * 使用 ThreadLocal 存储用户信息
     */
    private static final ThreadLocal<UserClientDto> USERS = new ThreadLocal<>();

    public void setUser(UserClientDto user) {
        USERS.set(user);
    }

    public UserClientDto getUser() {
        return USERS.get();
    }

    public void clear() {
        USERS.remove();
    }

}
