package com.sk.rts.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumValuesValidator implements ConstraintValidator<EnumValues, Object> {

    private String message;

    private Set<Object> values;

    @Override
    public void initialize(EnumValues enumValues) {
        Class<? extends EnumValue<?>> enumClass = enumValues.value();
        if (!enumClass.isEnum()) {
            return;
        }

        this.values = Arrays.stream(enumClass.getEnumConstants()).map(EnumValue::value).collect(Collectors.toSet());

        if (!StringUtils.hasText(this.message) && !this.values.isEmpty()) {
            this.message = "允许值：" + this.values.stream().map(Object::toString).collect(Collectors.joining(", ", "{", "}"));
        }
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || this.values.contains(value)) {
            return true;
        }

        if (context.getDefaultConstraintMessageTemplate().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(this.message).addConstraintViolation();
        }
        return false;
    }
}
