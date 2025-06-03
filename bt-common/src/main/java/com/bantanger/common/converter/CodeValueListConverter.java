package com.bantanger.common.converter;

import com.bantanger.common.model.CodeValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Try;
import jakarta.persistence.AttributeConverter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
@Slf4j
public class CodeValueListConverter implements AttributeConverter<List<CodeValue>, String> {

    @Override
    public String convertToDatabaseColumn(List<CodeValue> codeValueList) {
        return Try.of(() -> new ObjectMapper().writeValueAsString(codeValueList))
                .onFailure(e -> log.error("convertToDatabaseColumn json writing error", e))
                .getOrNull();
    }

    @Override
    public List<CodeValue> convertToEntityAttribute(String o) {
        return Try.of(() -> new ObjectMapper().readValue(o,
                new TypeReference<List<CodeValue>>() {}))
                .onFailure(e -> log.error("convertToEntityAttribute json reading error", e))
                .getOrNull();
    }
}
