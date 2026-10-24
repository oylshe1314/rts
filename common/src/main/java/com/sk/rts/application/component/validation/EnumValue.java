package com.sk.rts.application.component.validation;

public interface EnumValue<T> {

    String name();

    int ordinal();

    T value();
}
