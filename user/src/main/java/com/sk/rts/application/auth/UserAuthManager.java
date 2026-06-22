package com.sk.rts.application.auth;

import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.service.AuthService;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@NullMarked
@AllArgsConstructor
public class UserAuthManager implements ReactiveAuthenticationManager {

    private AuthService authService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        if ((!(authentication instanceof UserAuthToken authRequest))) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        String username = (String) authRequest.getPrincipal();
        String password = (String) authRequest.getCredentials();

        UserRemoteDetails remoteDetails = (UserRemoteDetails) authRequest.getDetails();
        if (remoteDetails == null) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        return authService.passwordLogin(username, password, remoteDetails).flatMap(authDetails -> authService.generateToken(authDetails).map(tokenDetails -> {
            UserAuthToken authResult = new UserAuthToken(authDetails, tokenDetails.getAccessToken());
            authResult.setDetails(tokenDetails);
            return authResult;
        }));
    }
}
