package com.bantanger.common.enums;

import java.util.stream.Stream;

/**
 * @author chensongmin
 * @description
 * @create 2025/1/4
 */
public interface BaseEnum<T extends Enum<T> & BaseEnum<T>> {

    /**
     * 获取 code 码，可无感存入数据库
     * @return
     */
    Integer getCode();

    /**
     * 获取业务编码名称
     * @return
     */
    String getName();

    /**
     * 根据 code 码获取业务枚举
     * @param enumClazz 枚举对象
     * @param code 枚举code码
     * @return 具体枚举值
     */
    static <T extends Enum<T> & BaseEnum<T>> T parseByCode(Class<T> enumClazz, Integer code) {
        return Stream.of(enumClazz.getEnumConstants())
                .filter(t -> t.getCode().intValue() == code.intValue())
                .findFirst()
                .orElse(null);
    }

}
