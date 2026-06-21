package com.sk.rts.application.entity.enums;

import lombok.Getter;

@Getter
public enum Status {

    disable(0, "禁用"),

    enable(1, "启用"),
    ;

    private final int value;
    private final String desc;

    Status(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static Status valueOf(Integer value) {
        return value == null || value == disable.value() ? disable : enable;
    }

    public static String desc(int value) {
        Status status = valueOf(value);
        return status == null ? "未知" : status.desc();
    }

    public static boolean enable(Integer value) {
        return value != null && value != 0;
    }

    public static boolean disable(Integer value) {
        return !enable(value);
    }

    public int value() {
        return value;
    }

    public String desc() {
        return this.desc;
    }
}
