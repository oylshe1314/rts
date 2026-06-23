package com.sk.rts.application.auth;

import com.sk.rts.application.service.AuthService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@NullMarked
public class UserCaptchaAuthManager extends UserAuthManager {

    public UserCaptchaAuthManager(AuthService authService) {
        super(authService);
    }

    @Override
    protected Mono<UserAuthDetails> authenticate(String principal, String credentials, UserRemoteDetails remoteDetails) {
        return authService.captchaLogin(principal, credentials, remoteDetails);
    }
}
