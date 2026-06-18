package com.sk.rts.application.auth;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@NullMarked
public class UserAuthToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final Object credentials;

    public UserAuthToken(Object principal, Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public UserAuthToken(UserAuthDetails authDetails, UserAccessToken accessToken) {
        super(authDetails.getAuthorities());
        this.principal = authDetails;
        this.credentials = accessToken;
        this.setAuthenticated(true);
    }
}
