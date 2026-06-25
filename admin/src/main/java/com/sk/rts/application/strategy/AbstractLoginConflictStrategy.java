package com.sk.rts.application.strategy;

import com.sk.rts.application.auth.AdminAccessToken;
import com.sk.rts.application.auth.AdminAuthDetails;
import com.sk.rts.application.auth.AdminAuthToken;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@NullMarked
public abstract class AbstractLoginConflictStrategy implements LoginConflictStrategy {

    @Override
    public Mono<Void> handleLoginConflict(Authentication authentication) {
        if (authentication instanceof AdminAuthToken authToken) {
            return resolve((AdminAuthDetails) authToken.getPrincipal(), (AdminAccessToken) authToken.getCredentials());
        } else {
            return Mono.empty();
        }
    }

    protected abstract Mono<Void> resolve(AdminAuthDetails authDetails, AdminAccessToken accessToken);
}
