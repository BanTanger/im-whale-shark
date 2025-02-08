package com.bantanger.jpa.converter;

import com.bantanger.common.enums.ValidStatus;
import jakarta.persistence.AttributeConverter;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public class ValidStatusConverter implements AttributeConverter<ValidStatus, Integer> {
    @Override
    public Integer convertToDatabaseColumn(ValidStatus validStatus) {
        return validStatus.getCode();
    }

    @Override
    public ValidStatus convertToEntityAttribute(Integer code) {
        return ValidStatus.of(code).orElse(null);
    }
}
