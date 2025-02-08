package com.bantanger.common.exception;

import com.bantanger.common.model.ValidateResult;
import java.util.List;
import lombok.Getter;

/**
 * @author gim
 */
@Getter
public class ValidationException extends RuntimeException {

    private final List<ValidateResult> result;

    public ValidationException(List<ValidateResult> list) {
        super();
        this.result = list;
    }
}
