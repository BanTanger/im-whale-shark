package com.bantanger.im.common.enums;

/**
 * @author BanTanger 半糖
 * @Date 2023/3/30 14:39
 */
public enum RouteHashMethodEnum {

    /**
     * TreeMap
     */
    TREE(1,"com.bantanger.im.service.route.algroithm.hash" +
            ".TreeMapConsistentHash"),

    /**
     * 自定义map
     */
    CUSTOMER(2,"com.bantanger.im.service.route.algroithm.hash.xxxx"),

    ;


    private int code;
    private String clazz;

    /**
     * 不能用 默认的 enumType b= enumType.values()[i]; 因为本枚举是类形式封装
     * @param ordinal
     * @return
     */
    public static RouteHashMethodEnum getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethodEnum.values().length; i++) {
            if (RouteHashMethodEnum.values()[i].getCode() == ordinal) {
                return RouteHashMethodEnum.values()[i];
            }
        }
        return null;
    }

    RouteHashMethodEnum(int code, String clazz){
        this.code=code;
        this.clazz=clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}
