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
     * Redis主机地址
     */
    private String host = "localhost";

    /**
     * Redis端口
     */
    private int port = 6379;

    /**
     * Redis数据库
     */
    private int database = 0;

    /**
     * 连接字符串
     */
    private Endpoint[] endpoints;

    /**
     * 用户
     */
    private String user;

    /**
     * 密码
     */
    private String password;

    @Getter
    @Setter
    public static class Endpoint {

        /**
         * Redis主机地址
         */
        private String host = "localhost";

        /**
         * Redis端口
         */
        private int port = 6379;

        /**
         * Redis数据库
         */
        private int database = 0;
    }
}
