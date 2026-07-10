package com.sk.rts.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateValidator implements ConstraintValidator<Date, String> {

    private DateTimeFormatter formatter;

    private String message;

    @Override
    public void initialize(Date date) {
        this.formatter = DateTimeFormatter.ofPattern(date.value());
        this.message = date.message();
        if (!StringUtils.hasText(this.message)) {
            this.message = "允许值：" + date.value();
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        try {
            LocalDate.parse(value, this.formatter);
            return true;
        } catch (Exception e) {
            if (context.getDefaultConstraintMessageTemplate().isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(this.message).addConstraintViolation();
            }
            return false;
        }
    }
}
