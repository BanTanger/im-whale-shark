package com.bantanger.im.common.converter;

import com.bantanger.im.common.enums.group.GroupStatusType;

import jakarta.persistence.AttributeConverter;

public class GroupStatusTypeConverter implements AttributeConverter<GroupStatusType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(GroupStatusType groupStatusType) {
        return groupStatusType.getCode();
    }

    @Override
    public GroupStatusType convertToEntityAttribute(Integer code) {
        return GroupStatusType.of(code).orElse(null);
    }
}
