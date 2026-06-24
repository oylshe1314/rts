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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

@SpringBootConfiguration
public class BeanConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public PathMatcher pathMatcher() {
        return new AntPathMatcher();
    }

    @Bean
    public DSLContext dslContext() {
        Settings settings = new Settings();
        settings.withRenderNamedParamPrefix("$");
        settings.setParamType(ParamType.NAMED);
        return DSL.using(SQLDialect.POSTGRES, settings);
    }
}

