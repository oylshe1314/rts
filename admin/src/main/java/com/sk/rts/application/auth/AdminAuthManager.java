package com.sk.rts.application.auth;

import com.sk.rts.application.component.TokenUtil;
import com.sk.rts.application.exception.ResponseStatus;
import com.sk.rts.application.exception.StandardStatusException;
import com.sk.rts.application.service.AuthService;
import com.sk.rts.application.util.CodecUtil;
import com.sk.rts.application.util.FeistelUtil;
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
public class AdminAuthManager implements ReactiveAuthenticationManager {

    private final TokenUtil tokenUtil;
    private final AuthService authService;

    @Override
    public Mono<Authentication> authenticate(Authentication authRequest) {
        if ((!(authRequest instanceof AdminAuthToken authToken))) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        String username = (String) authToken.getPrincipal();
        String password = (String) authToken.getCredentials();

        AdminRemoteDetails adminRemoteDetails = (AdminRemoteDetails) authToken.getDetails();
        if (adminRemoteDetails == null) {
            return Mono.error(new BadCredentialsException("", new StandardStatusException(ResponseStatus.internal_error)));
        }

        return authService.login(username, password, adminRemoteDetails).map(adminDetails -> {
            AdminAccessToken tokenDetail = tokenUtil.generate(CodecUtil.encode64(FeistelUtil.encode(adminDetails.getId())));
            return new AdminAuthToken(adminDetails, tokenDetail);
        });
    }
}
