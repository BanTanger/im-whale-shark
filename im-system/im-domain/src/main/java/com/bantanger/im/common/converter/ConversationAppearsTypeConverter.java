package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */
import com.bantanger.im.common.enums.conversation.ConversationType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConversationAppearsTypeConverter implements AttributeConverter<ConversationType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ConversationType conversation) {
        return conversation.getCode();
    }

    @Override
    public ConversationType convertToEntityAttribute(Integer code) {
        return ConversationType.of(code).orElse(null);
    }
}
