package com.sk.rts.application.entity.enums;

public enum State {

    disable(0, "text.status.disable", "禁用"),

    enable(1, "text.status.enable", "启用"),
    ;

    private final int value;
    private final String code;
    private final String desc;

    State(int value, String code, String desc) {
        this.value = value;
        this.code = code;
        this.desc = desc;
    }

    public int value() {
        return value;
    }

    public String code() {
        return code;
    }

    public String desc() {
        return this.desc;
    }

    public static State valueOf(Integer value) {
        return value == null || value == disable.value() ? disable : enable;
    }

    public static boolean enable(Integer value) {
        return value != null && value != 0;
    }

    public static boolean disable(Integer value) {
        return !enable(value);
    }
}
