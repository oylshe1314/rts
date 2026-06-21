package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAuthToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@NullMarked
public abstract class AbstractLoginConflictStrategy implements LoginConflictStrategy {

    private final ConcurrentMap<String, WebSession> sessions = new ConcurrentHashMap<>();

    protected void saveSession(String username, WebSession session) {
        sessions.put(username, session);
    }

    protected @Nullable WebSession getSession(String username) {
        return sessions.get(username);
    }

    protected void removeSession(String username) {
        sessions.remove(username);
    }

    @Override
    public Mono<Void> handleLoginConflict(ServerWebExchange exchange, Authentication authentication) {
        if ((!(authentication instanceof AdminAuthToken))) {
            return Mono.empty();
        }

        return resolve(exchange, (AdminAuthDetails) authentication.getPrincipal()).flatMap(newSession -> {
            saveSession(authentication.getName(), newSession);
            return Mono.empty();
        });
    }

    protected abstract Mono<WebSession> resolve(ServerWebExchange exchange, AdminAuthDetails adminAuthDetails);
}
