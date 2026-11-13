package com.sk.rts.application.entity.enums;


import com.sk.rts.application.component.validation.EnumValue;

public enum CaptchaType implements EnumValue<Integer> {
    @Deprecated
    image,
    login,
    bind,
    password,
    security,
    unbind,
    deregister,
    ;

    public Integer value() {
        return this.ordinal();
    }

    public static CaptchaType valueOf(Integer value) {
        if (value == null) {
            return null;
        }

        return switch (value) {
            case 0 -> CaptchaType.image;
            case 1 -> CaptchaType.login;
            case 2 -> CaptchaType.bind;
            case 3 -> CaptchaType.password;
            case 4 -> CaptchaType.security;
            case 5 -> CaptchaType.unbind;
            case 6 -> CaptchaType.deregister;
            default -> null;
        };
    }
}
