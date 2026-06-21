package com.sk.rts.application.entity.enums;

import lombok.Getter;

@Getter
public enum Gender {
    female("text.gender.female", "女"),
    male("text.gender.male", "男"),
    ;

    private final String code;
    private final String desc;

    Gender(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int value() {
        return ordinal();
    }
}
