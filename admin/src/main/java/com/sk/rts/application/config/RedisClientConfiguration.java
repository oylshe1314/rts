package com.sk.rts.application.config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootConfiguration
public class RedisClientConfiguration {

    @Bean
    public Redis redis(Vertx vertx, RedisClientProperties properties) {
        RedisOptions redisOptions = new RedisOptions();
        if (properties.getEndpoints() == null || properties.getEndpoints().length == 0) {
            if (properties.getDatabase() == 0) {
                redisOptions.setConnectionString(String.format("redis://%s:%d", properties.getHost(), properties.getPort()));
            } else {
                redisOptions.setConnectionString(String.format("redis://%s:%d/%d", properties.getHost(), properties.getPort(), properties.getDatabase()));
            }
        } else {
            for (RedisClientProperties.Endpoint endpoint : properties.getEndpoints()) {
                redisOptions.setConnectionString(String.format("redis://%s:%d", endpoint.getHost(), endpoint.getPort()));
            }
        }

        if (StringUtils.hasText(properties.getUser())) {
            redisOptions.setUser(properties.getUser());
        }

        if (StringUtils.hasText(properties.getPassword())) {
            redisOptions.setPassword(properties.getPassword());
        }

        return Redis.createClient(vertx, redisOptions);
    }

    @Bean
    public RedisAPI redisAPI(Redis redis) {
        return RedisAPI.api(redis);
    }
}
