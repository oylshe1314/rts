package com.sk.rts.application.auth;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;
import java.util.Objects;

@Getter
@Setter
@NullUnmarked
public class AuthDetails implements UserDetails, AuthSubject {

    private String subject;

    private Long userId;

    private Long deviceId;

    private String username;

    private String password;

    private Locale locale;

    private String ipAddress;

    private Collection<? extends GrantedAuthority> authorities;

    public void setRoles(Collection<String> roles) {
        this.authorities = roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    public Collection<String> getRoles() {
        return this.authorities.stream().map(GrantedAuthority::getAuthority).filter(Objects::nonNull).map(authority -> authority.substring(5)).toList();
    }
}
