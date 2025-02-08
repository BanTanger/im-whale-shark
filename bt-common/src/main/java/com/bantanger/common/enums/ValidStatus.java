package com.bantanger.common.enums;

import java.util.Optional;

/**
 * @author chensongmin
 * @description 有效性枚举
 * @create 2025/1/5
 */
public enum ValidStatus implements BaseEnum<ValidStatus> {

    // 无效
    INVALID(0, "invalid"),
    // 有效
    VALID(1, "valid"),

    ;

    ValidStatus(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private Integer code;
    private String name;

    @Override
    public Integer getCode() {
        return this.code;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public static Optional<ValidStatus> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(ValidStatus.class, code));
    }

}
