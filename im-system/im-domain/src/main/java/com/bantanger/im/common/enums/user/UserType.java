package com.bantanger.im.common.enums.user;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum UserType implements BaseEnum<UserType> {

    ORDINARY(1, "普通用户"),
    CUSTOMER(2, "客服"),
    ROBOT(3, "机器人"),
    APP_ADMIN(100, "APP管理员"),

    ;

    UserType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<UserType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(UserType.class, code));
    }

}
