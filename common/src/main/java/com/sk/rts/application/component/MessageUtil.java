package com.sk.rts.application.component;

import com.sk.rts.application.exception.StatusException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(MessageSource.class)
public class MessageUtil {

    private static MessageSource messageSource;

    public MessageUtil(MessageSource messageSource) {
        MessageUtil.messageSource = messageSource;
    }

    public static String getMessage(String code, Object... args) {
        return messageSource == null ? null : messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public static String getMessage(String code, String defaultMessage, Object... args) {
        String message = getMessage(code, args);
        return message != null ? message : defaultMessage;
    }

    public static String getMessage(StatusException statusException, Object... args) {
        return getMessage(statusException.getCode(), statusException.getMessage(), args);
    }

    public static String getMessage(StatusException statusException, String defaultMessage, Object... args) {
        return getMessage(statusException.getCode(), defaultMessage, args);
    }
}
