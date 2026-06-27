package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.config.TokenProperties;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.service.AuthService;
import com.sk.rts.application.strategy.LoginConflictStrategy;
import lombok.AllArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Component
@NullMarked
@AllArgsConstructor
public class AdminAuthManager implements ReactiveAuthenticationManager {

    private final AuthService authService;

    private final TokenUtil tokenUtil;

    private final LoginConflictStrategy loginConflictStrategy;

    @Override
    public Mono<Authentication> authenticate(Authentication authRequest) {
        if ((!(authRequest instanceof AdminAuthToken authToken))) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        String account = (String) authToken.getPrincipal();
        String password = (String) authToken.getCredentials();

        AdminRemoteDetails remoteDetails = (AdminRemoteDetails) authToken.getDetails();
        if (remoteDetails == null) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        return authService.login(account, password, remoteDetails).flatMap(authDetails -> {
            AdminAccessToken tokenDetail = tokenUtil.generate(AdminAuthUtil.buildSubject(authDetails.getAdminId()));
            AdminAuthToken authResult = new AdminAuthToken(authDetails, tokenDetail);
            authResult.setDetails(remoteDetails);
            return loginConflictStrategy.handleLoginConflict(authResult).thenReturn(authResult);
        });
    }
}
