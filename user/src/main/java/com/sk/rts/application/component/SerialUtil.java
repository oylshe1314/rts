package com.sk.rts.application.component;

import io.vertx.core.Future;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import io.vertx.redis.client.Response;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@AllArgsConstructor
public class SerialUtil {

    private final Redis redis;
    private final Random random;

    private Future<Long> nextIndex(String key) {
        Request request = Request.cmd(Command.INCRBY, key, 1);
        return redis.send(request).map(Response::toLong);
    }

    public Future<String> buildOrderNo() {
        return nextIndex("index:order:order").map(index -> String.format("%d%04d%04d", System.currentTimeMillis() / 1000 - 760320000, (index % 9999) + 1, random.nextLong(9999) + 1));
    }
}
