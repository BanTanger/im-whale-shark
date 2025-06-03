package com.bantanger.im.common.converter;

/**
 * @author chensongmin
 * @description
 * @date 2025/6/3
 */

import com.bantanger.im.common.enums.conversation.ConversationNoticeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ConversationNoticeTypeConverter implements AttributeConverter<ConversationNoticeType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ConversationNoticeType conversationNoticeType) {
        return conversationNoticeType.getCode();
    }

    @Override
    public ConversationNoticeType convertToEntityAttribute(Integer code) {
        return ConversationNoticeType.of(code).orElse(null);
    }
}