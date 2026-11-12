package com.sk.rts.application.auth;

import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.service.AuthService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Mono;

@NullMarked
@AllArgsConstructor
public abstract class UserAuthManager implements ReactiveAuthenticationManager {

    protected final AuthService authService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if ((!(authentication instanceof UserAuthToken authRequest))) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        String principal = (String) authRequest.getPrincipal();
        String credentials = (String) authRequest.getCredentials();

        UserRemoteDetails remoteDetails = (UserRemoteDetails) authRequest.getDetails();

        return authenticate(principal, credentials, remoteDetails);
    }

    protected abstract Mono<Authentication> authenticate(String principal, String credentials, UserRemoteDetails remoteDetails);
}
