package com.bantanger.im.common.enums.route;

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
    LOOP(2, "com.bantanger.im.service.route.algroithm.loop.LoopHandler"),
    /**
     * 一致性 HASH
     */
    HASH(3, "com.bantanger.im.service.route.algroithm.hash.ConsistentHashHandler"),
    ;
    private int code;
    private String clazz;


    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static UrlRouteModelEnum getHandler(int ordinal) {
        for (int i = 0; i < UrlRouteModelEnum.values().length; i++) {
            if (UrlRouteModelEnum.values()[i].getCode() == ordinal) {
                return UrlRouteModelEnum.values()[i];
            }
        }
        return null;
    }

    UrlRouteModelEnum(int code, String clazz) {
        this.code = code;
        this.clazz = clazz;
    }

    public int getCode() {
        return code;
    }

    public String getClazz() {
        return clazz;
    }

}
