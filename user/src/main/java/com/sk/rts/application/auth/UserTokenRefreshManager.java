package com.sk.rts.application.auth;

import com.bds.user.application.service.AuthService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@NullMarked
@AllArgsConstructor
public class UserTokenRefreshManager extends UserAuthManager {

    private final AuthService authService;

    @Override
    protected Mono<Authentication> authenticate(String principal, String credentials, UserRemoteDetails remoteDetails) {
        return authService.tokenRefresh(principal, credentials, remoteDetails);
    }
}
