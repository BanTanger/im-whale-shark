package com.bantanger.im.common.enums;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum WhetherType implements BaseEnum<WhetherType> {

    YES(0, "是"),
    NO(1, "否"),

    ;

    WhetherType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<WhetherType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(WhetherType.class, code));
    }

}
