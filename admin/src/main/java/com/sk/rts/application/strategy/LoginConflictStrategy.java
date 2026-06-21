package com.sk.rts.application.strategy;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@NullMarked
public interface LoginConflictStrategy {

    Mono<Void> handleLoginConflict(ServerWebExchange exchange, Authentication authentication);
}
