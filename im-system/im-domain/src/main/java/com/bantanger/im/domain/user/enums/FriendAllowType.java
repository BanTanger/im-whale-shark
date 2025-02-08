package com.bantanger.im.domain.user.enums;

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
@Getter
public enum FriendAllowType implements BaseEnum<FriendAllowType> {

    NO_VERIFY(1, "无需验证"),
    VERIFY(2, "需要验证"),

    ;

    FriendAllowType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<FriendAllowType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(FriendAllowType.class, code));
    }

}
