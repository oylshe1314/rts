package com.sk.rts.application.config;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.ClientBuilder;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@NullMarked
@SpringBootConfiguration
public class SqlClientConfiguration {

    @Bean
    public Pool pool(Vertx vertx, SqlClientProperties properties) {
        PgConnectOptions connectOptions = new PgConnectOptions();
        connectOptions.setHost(properties.getHost());
        connectOptions.setPort(properties.getPort());
        connectOptions.setUser(properties.getUser());
        connectOptions.setPassword(properties.getPassword());
        connectOptions.setDatabase(properties.getDatabase());

        PoolOptions poolOptions = new PoolOptions();
        if (properties.getPool() != null) {
            if (properties.getPool().getMaxSize() > 0) {
                poolOptions.setMaxSize(properties.getPool().getMaxSize());
            } else {
                poolOptions.setMaxSize(8);
            }
            if (properties.getPool().getConnectionTimeout() > 0) {
                poolOptions.setConnectionTimeout(properties.getPool().getConnectionTimeout());
                poolOptions.setConnectionTimeoutUnit(TimeUnit.MILLISECONDS);
            }
            if (properties.getPool().getIdleTimeout() > 0) {
                poolOptions.setIdleTimeout(properties.getPool().getIdleTimeout());
                poolOptions.setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
            }
            if (properties.getPool().getMaxLifetime() > 0) {
                poolOptions.setMaxLifetime(properties.getPool().getMaxLifetime());
                poolOptions.setMaxLifetimeUnit(TimeUnit.MILLISECONDS);
            }
        }

        ClientBuilder<Pool> builder = PgBuilder.pool();
        builder.using(vertx);
        builder.with(poolOptions);
        builder.connectingTo(connectOptions);

        return builder.build();
    }
}
