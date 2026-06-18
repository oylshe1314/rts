package com.sk.rts.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class NullOrNotBlankValidator implements ConstraintValidator<NullOrNotBlank, String> {

    private String message;

    @Override
    public void initialize(NullOrNotBlank in) {
        this.message = in.message();
        if (!StringUtils.hasText(this.message)) {
            this.message = "值只能为null或者非空白字符串";
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || StringUtils.hasText(value)) {
            return true;
        }

        if (context.getDefaultConstraintMessageTemplate().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(this.message).addConstraintViolation();
        }
        return false;
    }
}
