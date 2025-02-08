package com.bantanger.common.mapper;

import java.time.Instant;
import java.util.Objects;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public class DateMapper {

    public Long asLong(Instant date) {
        if (Objects.nonNull(date)) {
            return date.toEpochMilli();
        }
        return null;
    }

    public Instant asInstant(Long date) {
        if (Objects.nonNull(date)) {
            return Instant.ofEpochMilli(date);
        }
        return null;
    }
}