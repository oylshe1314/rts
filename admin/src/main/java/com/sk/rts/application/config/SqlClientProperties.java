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
    private String host = "localhost";

    /**
     * 数据库端口
     */
    private int port = 5432;

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

    /**
     * 连接池配置
     */
    private PoolProperties pool;

    @Getter
    @Setter
    public static class PoolProperties {

        /**
         * 连接池最大数量
         */
        private int maxSize;

        /**
         * 连接超时时间(毫秒)
         */
        private int connectionTimeout;

        /**
         * 空闲超时时间(毫秒)
         */
        private int idleTimeout;

        /**
         * 最大存活时间(毫秒)
         */
        private int maxLifetime;
    }
}
