package com.sk.rts.application.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vertx")
public class VertxProperties {

    /**
     * 集群是否启动
     */
    private Boolean clusterEnabled;

    /**
     * Vertx成员列表
     */
    private List<String> clusterMembers;

    /**
     * Vertx成员端口，如果设置成员列表，测必须设置成员端口
     */
    private Integer memberPort;
}
