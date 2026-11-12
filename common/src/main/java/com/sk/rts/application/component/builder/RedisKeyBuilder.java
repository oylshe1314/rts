package com.sk.rts.application.component.builder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyBuilder {
    private static final String SERVER = "rts";
    private static final String SEQUENCE = ":";

    private final String prefix;

    public RedisKeyBuilder(@Value("${spring.application.name}") String appName) {
        prefix = SERVER + SEQUENCE + appName;
    }

    public String build(Object... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        for (Object arg : args) {
            sb.append(SEQUENCE);
            sb.append(arg);
        }
        return sb.toString();
    }
}
