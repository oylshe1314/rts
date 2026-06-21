package com.sk.rts.application.entity.enums;

public enum Platform {
    web,
    windows,
    macos,
    ios,
    android,
    linux,
    fuchsia,
    ;

    public static Platform parse(String platform) {
        return switch (platform) {
            case "web" -> web;
            case "windows" -> windows;
            case "macos" -> macos;
            case "ios" -> ios;
            case "android" -> android;
            case "linux" -> linux;
            case "fuchsia" -> fuchsia;
            default -> null;
        };
    }
}
