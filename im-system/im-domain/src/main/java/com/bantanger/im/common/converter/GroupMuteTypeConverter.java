package com.bantanger.im.common.converter;

import com.bantanger.im.common.enums.group.GroupMuteType;
import jakarta.persistence.AttributeConverter;

public class GroupMuteTypeConverter implements AttributeConverter<GroupMuteType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(GroupMuteType groupMuteType) {
        return groupMuteType.getCode();
    }

    @Override
    public GroupMuteType convertToEntityAttribute(Integer code) {
        return GroupMuteType.of(code).orElse(null);
    }
}
