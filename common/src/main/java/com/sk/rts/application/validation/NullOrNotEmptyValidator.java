package com.sk.rts.application.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;

public class NullOrNotEmptyValidator implements ConstraintValidator<NullOrNotEmpty, Collection> {

    private String message;

    @Override
    public void initialize(NullOrNotEmpty in) {
        this.message = in.message();
        if (!StringUtils.hasText(this.message)) {
            this.message = "值只能为null或者非空白集合";
        }
    }

    @Override
    public boolean isValid(Collection value, ConstraintValidatorContext context) {
        if (value == null || !CollectionUtils.isEmpty(value)) {
            return true;
        }

        if (context.getDefaultConstraintMessageTemplate().isEmpty()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(this.message).addConstraintViolation();
        }
        return false;
    }
}
