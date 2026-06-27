package com.sk.rts.application.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;

@Getter
@NullMarked
@AllArgsConstructor
public class ApiPatternAuthority implements GrantedAuthority {

    private final String authority;

    @Override
    public int hashCode() {
        return authority.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ApiPatternAuthority patternAuthority && this.authority.equals(patternAuthority.getAuthority());
    }

    @Override
    public String toString() {
        return authority;
    }
}

