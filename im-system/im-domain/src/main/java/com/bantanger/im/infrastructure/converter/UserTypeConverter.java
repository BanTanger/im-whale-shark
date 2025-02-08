package com.bantanger.im.infrastructure.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
import com.bantanger.im.domain.user.enums.UserType;
import jakarta.persistence.AttributeConverter;

public class UserTypeConverter implements AttributeConverter<UserType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(UserType userType) {
        return userType.getCode();
    }

    @Override
    public UserType convertToEntityAttribute(Integer code) {
        return UserType.of(code).orElse(null);
    }
}
