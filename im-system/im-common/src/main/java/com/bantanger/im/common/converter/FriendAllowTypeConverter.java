package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/2/7
 */
import com.bantanger.im.common.enums.user.FriendAllowType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class FriendAllowTypeConverter implements AttributeConverter<FriendAllowType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(FriendAllowType friendAllowType) {
        return friendAllowType.getCode();
    }

    @Override
    public FriendAllowType convertToEntityAttribute(Integer code) {
        return FriendAllowType.of(code).orElse(null);
    }
}
