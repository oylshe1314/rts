package com.sk.rts.application.entity.enums;

public enum MenuType {

    dir(1, "目录"),

    menu(2, "菜单"),

    api(3, "接口"),
    ;

    private final int value;
    private final String desc;

    MenuType(int value, String desc) {
        this.value = value;
        this.desc = desc;
    }

    public static MenuType valueOf(int value) {
        return switch (value) {
            case 1 -> dir;
            case 2 -> menu;
            case 3 -> api;
            default -> null;
        };
    }

    public static MenuType valueOf(Integer value) {
        if (value == null) {
            return null;
        }
        return valueOf(value.intValue());
    }

    public static String desc(int value) {
        MenuType type = valueOf(value);
        return type == null ? "未知" : type.desc();
    }

    public int value() {
        return value;
    }

    public String desc() {
        return this.desc;
    }
}
