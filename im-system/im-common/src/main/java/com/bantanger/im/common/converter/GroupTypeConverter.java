package com.bantanger.im.common.converter;

import com.bantanger.im.common.enums.group.GroupType;
import jakarta.persistence.AttributeConverter;

public class GroupTypeConverter implements AttributeConverter<GroupType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(GroupType groupType) {
        return groupType.getCode();
    }

    @Override
    public GroupType convertToEntityAttribute(Integer code) {
        return GroupType.of(code).orElse(null);
    }
}