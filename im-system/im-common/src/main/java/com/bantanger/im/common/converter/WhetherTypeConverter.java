package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */
import com.bantanger.im.common.enums.WhetherType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WhetherTypeConverter implements AttributeConverter<WhetherType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(WhetherType whetherType) {
        return whetherType.getCode();
    }

    @Override
    public WhetherType convertToEntityAttribute(Integer code) {
        return WhetherType.of(code).orElse(null);
    }
}
