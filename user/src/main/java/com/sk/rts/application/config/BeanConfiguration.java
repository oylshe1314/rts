package com.sk.rts.application.config;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.ParamType;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Random;

@SpringBootConfiguration
public class BeanConfiguration {

    @Bean
    public Random random() {
        return new Random(System.currentTimeMillis());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DSLContext dslContext() {
        Settings settings = new Settings();
        settings.withRenderNamedParamPrefix("$");
        settings.setParamType(ParamType.NAMED);
        return DSL.using(SQLDialect.POSTGRES, settings);
    }
}

