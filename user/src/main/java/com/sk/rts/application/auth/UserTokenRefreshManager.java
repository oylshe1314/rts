package com.sk.rts.application.auth;

import com.sk.rts.application.service.AuthService;
import com.sk.rts.application.util.ReactiveUtil;
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
        return ReactiveUtil.toMono(authService.tokenRefresh(principal, credentials, remoteDetails));
    }
}
