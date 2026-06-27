package com.sk.rts.application.exception;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class ExceptionUtil {

    public static <T extends Exception> @Nullable T extractException(Throwable throwable, Class<T> clazz) {
        while (throwable != null) {
            if (clazz.isInstance(throwable)) {
                return (T) throwable;
            }

            throwable = throwable.getCause();
        }
        return null;
    }
}
