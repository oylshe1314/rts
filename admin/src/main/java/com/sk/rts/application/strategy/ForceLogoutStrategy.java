package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.exception.StandardStatusException;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

@NullMarked
public class ForceLogoutStrategy extends AbstractLoginConflictStrategy {

    @Override
    protected Mono<WebSession> resolve(ServerWebExchange exchange, AdminAuthDetails adminAuthDetails) throws StandardStatusException {
        WebSession session = getSession(adminAuthDetails.getUsername());

        if (session == null) {
            return exchange.getSession();
        }

        if (session.isExpired()) {
            return exchange.getSession();
        }

        return session.invalidate().then(exchange.getSession());
    }
}
