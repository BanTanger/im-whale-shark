package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/29 17:48
 */
public enum UrlRouteModelEnum {
    /**
     * 随机
     */
    RAMDOM(1, "com.bantanger.im.service.route.algroithm.random.RandomHandler"),
    /**
     * 轮询
     */
    LOOP(2, "com.bantanger.im.service.route.algroithm.random.LoopHandler"),
    /**
     * 一致性 HASH
     */
    HASH(3, "com.bantanger.im.service.route.algroithm.random.ConsistentHashHandler"),
    ;
    private int code;
    private String clazz;

    UrlRouteModelEnum(int code, String clazz) {
        this.code = code;
        this.clazz = clazz;
    }
}
