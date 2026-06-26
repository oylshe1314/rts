package com.sk.rts.application.config;

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
    private String loginConflictStrategy = "com.sk.rts.application.strategy.DenyLoginStrategy";
}
