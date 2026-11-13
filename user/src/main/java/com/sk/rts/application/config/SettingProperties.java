package com.sk.rts.application.config;

import com.sk.rts.application.entity.enums.CaptchaType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "rts.setting")
public class SettingProperties {

    @Setter(onMethod_ = @Value("${spring.profiles.active}"))
    private String active;

    /**
     * 不同类型的验证码长度, 类型：login, bind, password, security, deregister
     */
    private Map<CaptchaType, Integer> captchaLength = new HashMap<>();

    /**
     * 默认验证码长度
     */
    private Integer captchaDefaultLength = 6;

    /**
     * 不同类型的验证码有效期, 类型：login, bind, password, security, deregister
     */
    private Map<CaptchaType, Duration> captchaExpiration = new HashMap<>();

    /**
     * 默认验证码有效期
     */
    private Duration captchaDefaultExpiration = Duration.ofMinutes(10L);
}
