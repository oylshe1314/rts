package com.sk.rts.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "client.sql")
public class SqlClientProperties {

    /**
     * 数据库主机地址
     */
    private String host;

    /**
     * 数据库端口
     */
    private int port;

    /**
     * 数据库用户
     */
    private String user;

    /**
     * 数据库用户密码
     */
    private String password;

    /**
     * 数据库名称
     */
    private String database;
}
