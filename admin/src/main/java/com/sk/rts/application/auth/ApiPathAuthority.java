package com.sk.rts.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;

@Getter
@NullMarked
@AllArgsConstructor
public class ApiPathAuthority implements GrantedAuthority {

    private final String authority;

    public String getAuthority() {
        return authority;
    }
}

