package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */
import com.bantanger.im.common.enums.message.MsgType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MsgTypeConverter implements AttributeConverter<MsgType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(MsgType msgType) {
        return msgType.getCode();
    }

    @Override
    public MsgType convertToEntityAttribute(Integer code) {
        return MsgType.of(code).orElse(null);
    }
}
