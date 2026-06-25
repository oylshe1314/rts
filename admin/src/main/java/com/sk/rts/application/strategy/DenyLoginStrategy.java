package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAccessToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthUtil;
import com.sk.rts.application.exception.StandardStatusException;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

@NullMarked
@AllArgsConstructor
public class DenyLoginStrategy extends AbstractLoginConflictStrategy {

    private final Redis redis;

    @Override
    public Mono<Void> resolve(AdminAuthDetails authDetails, AdminAccessToken accessToken) {
        long adminId = authDetails.getAdminId();
        String subject = accessToken.getSubject();

        Request request = Request.cmd(Command.EXISTS, AdminAuthUtil.buildDetailsKey(adminId), AdminAuthUtil.buildTokenKey(subject));

        return Mono.create(sink -> redis.send(request).onFailure(sink::error).onSuccess(response -> {
            if (response.toLong() < 2) {
                sink.success();
            } else {
                sink.error(new StandardStatusException("用户已在其他设备登录，请先退出其他设备或者等待会话超时"));
            }
        }));
    }
}
