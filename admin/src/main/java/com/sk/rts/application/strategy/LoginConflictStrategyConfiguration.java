package com.sk.rts.application.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootConfiguration
public class LoginConflictStrategyConfiguration {

    private final String strategyClass;
    private final ApplicationContext applicationContext;

    @Autowired
    public LoginConflictStrategyConfiguration(@Value("${admin.login.conflict.strategy:com.sk.rts.application.strategy.DenyLoginStrategy}") String strategyClass, ApplicationContext applicationContext) {
        this.strategyClass = strategyClass;
        this.applicationContext = applicationContext;
    }

    @Bean
    public LoginConflictStrategy loginConflictStrategy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName(strategyClass);
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        return (LoginConflictStrategy) beanFactory.createBean(clazz);
    }
}
