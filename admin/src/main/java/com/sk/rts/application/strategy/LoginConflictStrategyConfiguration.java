package com.sk.rts.application.strategy;

import com.sk.rts.application.config.SettingProperties;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@AllArgsConstructor
@SpringBootConfiguration
public class LoginConflictStrategyConfiguration {

    private ApplicationContext applicationContext;
    private SettingProperties settingProperties;

    @Bean
    public LoginConflictStrategy loginConflictStrategy() throws ClassNotFoundException {
        Class<?> clazz = Class.forName(settingProperties.getLoginConflictStrategy());
        DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
        return (LoginConflictStrategy) beanFactory.createBean(clazz);
    }
}
