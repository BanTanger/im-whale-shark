package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
import com.bantanger.im.common.enums.user.UserType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
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
