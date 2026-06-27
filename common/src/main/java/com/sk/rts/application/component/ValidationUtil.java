package com.sk.rts.application.component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@NullMarked
@AllArgsConstructor
public class ValidationUtil {

    private final Validator validator;

    public <T> boolean validate(T object) {
        Set<ConstraintViolation<T>> set = validator.validate(object);
        if (set.isEmpty()) {
            return true;
        }

        List<String> messages = new ArrayList<>();
        for (ConstraintViolation<T> violation : set) {
            messages.add(String.format("参数: %s, %s", violation.getPropertyPath().toString(), violation.getMessage()));
        }
        String message = String.join(", ", messages);
        log.warn("参数错误, {}", message);
        return false;
    }
}
