package com.bantanger.im.common.enums.group;

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum GroupType implements BaseEnum<GroupType> {

    PRIVATE(1, "私有群（类似微信）"),

    PUBLIC(2, "公开群(类似qq）"),

    ;

    GroupType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<GroupType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(GroupType.class, code));
    }

}