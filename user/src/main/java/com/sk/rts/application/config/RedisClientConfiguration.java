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
        switch (properties.getConnectionStrings().length) {
            case 0:
                break;
            case 1:
                redisOptions.setType(RedisClientType.STANDALONE);
                redisOptions.setConnectionString(properties.getConnectionStrings()[0]);
                break;
            default:
                redisOptions.setType(RedisClientType.CLUSTER);
                for (String connectionString : properties.getConnectionStrings()) {
                    redisOptions.addConnectionString(connectionString);
                }
                break;
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
