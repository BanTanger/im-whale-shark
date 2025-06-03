package com.bantanger.im.common.converter;

import com.bantanger.im.common.enums.group.GroupMemberRoleType;
import jakarta.persistence.AttributeConverter;

public class GroupMemberRoleTypeConverter implements AttributeConverter<GroupMemberRoleType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(GroupMemberRoleType groupMemberRoleType) {
        return groupMemberRoleType.getCode();
    }

    @Override
    public GroupMemberRoleType convertToEntityAttribute(Integer code) {
        return GroupMemberRoleType.of(code).orElse(null);
    }
}