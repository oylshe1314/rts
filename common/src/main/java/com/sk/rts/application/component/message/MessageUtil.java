package com.sk.rts.application.component.message;

import com.sk.rts.application.exception.StatusException;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Locale;

@NullMarked
public class MessageUtil {

    @Setter
    @Nullable
    private static MessageSource messageSource;

    public static String getMessage(Locale locale, String code, Object... args) {
        return getMessage(locale, code, code, args);
    }

    public static String getMessage(Locale locale, String code, String defaultMessage, Object... args) {
        if (messageSource == null) {
            return defaultMessage;
        }
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            return defaultMessage;
        }
    }

    public static String getMessage(Locale locale, StatusException statusException, Object... args) {
        return getMessage(locale, statusException.getCode(), statusException.getMessage(), args);
    }

    public static String getMessage(Locale locale, StatusException statusException, String defaultMessage, Object... args) {
        return getMessage(locale, statusException.getCode(), defaultMessage, args);
    }

    private static Locale getLocale(ContextView contextView) {
        Locale locale = contextView.get(ServerWebExchange.class).getLocaleContext().getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return locale;
    }

    public static Mono<String> getMessage(String code, Object... args) {
        return getMessage(code, code, args);
    }

    public static Mono<String> getMessage(String code, String defaultMessage, Object... args) {
        return Mono.deferContextual(contextView -> {
            if (messageSource == null) {
                return Mono.just(defaultMessage);
            } else {
                Locale locale = getLocale(contextView);
                try {
                    String message = messageSource.getMessage(code, args, locale);
                    if (code.equals(message)) {
                        return Mono.just(defaultMessage);
                    }
                    return Mono.just(message);
                } catch (NoSuchMessageException e) {
                    return Mono.just(defaultMessage);
                }
            }
        });
    }

    public static Mono<String> getMessage(StatusException statusException, Object... args) {
        return getMessage(statusException.getCode(), statusException.getMessage(), args);
    }

    public static Mono<String> getMessage(StatusException statusException, String defaultMessage, Object... args) {
        return getMessage(statusException.getCode(), defaultMessage, args);
    }
}
