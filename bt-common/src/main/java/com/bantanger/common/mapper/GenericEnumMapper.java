package com.bantanger.common.mapper;

import com.bantanger.common.enums.ValidStatus;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public class GenericEnumMapper {

    public Integer asInteger(ValidStatus status) {
        return status.getCode();
    }

    public ValidStatus asValidStatus(Integer code) {
        return ValidStatus.of(code).orElse(ValidStatus.INVALID);
    }
}