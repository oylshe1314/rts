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

    public UserAuthToken(AuthDetails authDetails, TokenDetails tokenDetails) {
        super(authDetails.getAuthorities());
        this.principal = authDetails;
        this.credentials = tokenDetails;
        this.setAuthenticated(true);
    }

    @Override
    public String getName() {
        if (isAuthenticated()) {
            return ((AuthDetails) principal).getUsername();
        } else {
            return (String) principal;
        }
    }

    @Override
    public Object getDetails() {
        Object details = super.getDetails();
        assert details != null;
        return details;
    }
}
