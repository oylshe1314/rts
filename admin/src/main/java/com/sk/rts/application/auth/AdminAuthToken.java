package com.sk.rts.application.auth;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@NullMarked
public class AdminAuthToken extends AbstractAuthenticationToken {

    private final Object principal;
    private final Object credentials;

    public AdminAuthToken(Object principal, Object credentials) {
        super((Collection<? extends GrantedAuthority>) null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public AdminAuthToken(AdminAuthDetails adminAuthDetails, AdminAccessToken tokenDetail) {
        super(adminAuthDetails.getAuthorities());
        this.principal = adminAuthDetails;
        this.credentials = tokenDetail;
        this.setAuthenticated(true);
    }
}

