package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAccessToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import org.jspecify.annotations.NullMarked;
import reactor.core.publisher.Mono;

@NullMarked
public class ForceLogoutStrategy extends AbstractLoginConflictStrategy {

    @Override
    public Mono<Void> resolve(AdminAuthDetails authDetails, AdminAccessToken accessToken) {
        return Mono.empty();
    }
}
