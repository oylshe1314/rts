package com.sk.rts.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class EnumOrdinalsValidator implements ConstraintValidator<EnumOrdinals, Integer> {

    private String message;

    private Set<Integer> values;

    @Override
    public void initialize(EnumOrdinals enumOrdinals) {
        Class<? extends Enum<?>> enumClass = enumOrdinals.value();

        this.values = Arrays.stream(enumClass.getEnumConstants()).map(Enum::ordinal).collect(Collectors.toSet());

        if (!StringUtils.hasText(this.message) && !this.values.isEmpty()) {
            this.message = "允许值：" + this.values.stream().map(String::valueOf).collect(Collectors.joining(", ", "{", "}"));
        }
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
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
