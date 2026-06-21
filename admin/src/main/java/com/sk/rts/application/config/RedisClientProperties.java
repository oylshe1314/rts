package com.sk.rts.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "client.redis")
public class RedisClientProperties {

    /**
     * 连接字符串
     */
    private String[] connectionStrings;

    /**
     * 用户
     */
    private String user;

    /**
     * 密码
     */
    private String password;
}
