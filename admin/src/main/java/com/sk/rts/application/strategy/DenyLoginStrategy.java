package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.exception.StandardStatusException;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@NullMarked
public class DenyLoginStrategy extends AbstractLoginConflictStrategy {

    @Override
    protected Mono<WebSession> resolve(ServerWebExchange exchange, AdminAuthDetails adminAuthDetails) {
        WebSession session = getSession(adminAuthDetails.getUsername());
        if (session == null) {
            return exchange.getSession();
        }

        if (session.isExpired()) {
            return exchange.getSession();
        }

        return Mono.error(new StandardStatusException("用户已在其他设备登录，请先退出其他设备或者等待会话超时"));
    }
}
