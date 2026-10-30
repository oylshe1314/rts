package com.sk.rts.application.strategy;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@NullMarked
public interface LoginConflictStrategy {

    Mono<Void> handleLoginConflict(Authentication authentication);
}
