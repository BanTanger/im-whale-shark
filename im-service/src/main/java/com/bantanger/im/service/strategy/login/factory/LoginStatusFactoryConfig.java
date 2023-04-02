package com.bantanger.im.service.strategy.login.factory;

import com.bantanger.im.common.enums.DeviceMultiLoginEnum;
import com.bantanger.im.service.strategy.login.LoginStatus;
import com.bantanger.im.service.strategy.login.impl.OneClientLoginStatus;
import com.bantanger.im.service.strategy.login.impl.ThreeClientLoginStatus;
import com.bantanger.im.service.strategy.login.impl.TwoClientLoginStatus;
import com.bantanger.im.service.strategy.login.impl.AllClientLoginStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/27 15:38
 */
public class LoginStatusFactoryConfig {

    public static Map<Integer, LoginStatus> LoginStatusMap = new ConcurrentHashMap<>();

    public static void init() {
        LoginStatusMap.put(DeviceMultiLoginEnum.ONE.getCode(), new OneClientLoginStatus());
        LoginStatusMap.put(DeviceMultiLoginEnum.TWO.getCode(), new TwoClientLoginStatus());
        LoginStatusMap.put(DeviceMultiLoginEnum.THREE.getCode(), new ThreeClientLoginStatus());
        LoginStatusMap.put(DeviceMultiLoginEnum.ALL.getCode(), new AllClientLoginStatus());
    }
}
