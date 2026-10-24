package com.sk.rts.application.config;

import com.sk.rts.application.strategy.LoginConflictStrategy;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties("rts.admin.setting")
public class SettingProperties {

    /**
     * 登录冲突策略
     */
    private Class<? extends LoginConflictStrategy> loginConflictStrategy = com.sk.rts.application.strategy.DenyLoginStrategy.class;
}
