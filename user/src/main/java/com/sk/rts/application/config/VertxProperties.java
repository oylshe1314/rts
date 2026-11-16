package com.sk.rts.application.config;

import io.vertx.core.Deployable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * Vert.x Verticle组合发布实例数
     */
    private Map<Class<? extends Deployable>, Integer> deploymentInstances = new HashMap<>();
}
