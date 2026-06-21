package com.sk.rts.application.service;

import com.sk.rts.application.auth.UserAuthDetails;
import com.sk.rts.application.auth.UserRemoteDetails;
import com.sk.rts.application.auth.UserTokenDetails;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@NullMarked
@AllArgsConstructor
public class AuthService {

    public Mono<UserAuthDetails> passwordLogin(String username, String password, @Nullable UserRemoteDetails remoteDetails) {
        throw new UnsupportedOperationException("Not implements.");
    }

    public Mono<UserAuthDetails> captchaLogin(String username, String password, @Nullable UserRemoteDetails remoteDetails) {
        throw new UnsupportedOperationException("Not implements.");
    }

    public Mono<UserTokenDetails> generateToken(UserAuthDetails authDetails) {
        throw new UnsupportedOperationException("Not implements.");
    }
}
