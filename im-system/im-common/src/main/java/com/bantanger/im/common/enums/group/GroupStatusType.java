package com.bantanger.im.common.enums.group;

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum GroupStatusType implements BaseEnum<GroupStatusType> {

    NORMAL(1, "正常"),
    DESTROY(2, "解散"),
    FORBIDDEN(3, "封禁"),

    ;

    GroupStatusType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<GroupStatusType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(GroupStatusType.class, code));
    }

}
