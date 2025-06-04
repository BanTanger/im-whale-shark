package com.bantanger.im.common.enums.group;

import com.bantanger.common.enums.BaseEnum;
import java.util.Optional;
import lombok.Getter;

@Getter
public enum GroupMemberRoleType implements BaseEnum<GroupMemberRoleType> {

    /**
     * 普通成员
     */
    MEMBER(0, "普通成员"),

    /**
     * 管理员
     */
    MANAGER(1, "管理员"),

    /**
     * 群主
     */
    OWNER(2, "群主"),

    /**
     * 离开
     */
    LEAVE(3, "退出群聊用户");

    ;

    GroupMemberRoleType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    private final Integer code;
    private final String name;

    public static Optional<GroupMemberRoleType> of(Integer code) {
        return Optional.ofNullable(BaseEnum.parseByCode(GroupMemberRoleType.class, code));
    }

}