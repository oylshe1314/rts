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
        poolOptions.setMaxSize(10);

        ClientBuilder<Pool> builder = PgBuilder.pool();
        builder.using(vertx);
        builder.with(poolOptions);
        builder.connectingTo(connectOptions);

        return builder.build();
    }
}
