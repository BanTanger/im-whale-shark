package com.bantanger.jpa.support;

import com.bantanger.common.exception.ValidationException;
import com.bantanger.common.model.ValidateResult;
import com.bantanger.common.validator.ValidateGroup;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * @author chensongmin
 * @description
 * @date 2025/1/8
 */
public abstract class BaseEntityOperation implements EntityOperation {

    static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public <T> void doValidate(T t, Class<? extends ValidateGroup> group) {
        Set<ConstraintViolation<T>> constraintViolations = validator.validate(t, group, Default.class);
        if (!ObjectUtils.isEmpty(constraintViolations)) {
            List<ValidateResult> results = constraintViolations.stream()
                .map(cv -> new ValidateResult(cv.getPropertyPath().toString(), cv.getMessage()))
                .collect(Collectors.toList());
            throw new ValidationException(results);
        }
    }
}
