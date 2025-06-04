package com.bantanger.im.common.enums.group;

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum GroupMuteType implements BaseEnum<GroupMuteType> {

    NOT_MUTE(0, "正常"),
    ALL_MUTE(1, "全体禁言(仅群主能说话)"),
    MEMBER_MUTE(2, "成员禁言(群主和管理员能说话)"),
    ;

    GroupMuteType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<GroupMuteType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(GroupMuteType.class, code));
    }

}
