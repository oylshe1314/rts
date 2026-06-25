package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAuthToken;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@NullMarked
public interface LoginConflictStrategy {

    Mono<Void> handleLoginConflict(Authentication authentication);
}
