package com.sk.rts.application.auth;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullUnmarked;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Locale;

@Getter
@Setter
@NullUnmarked
public class AuthDetails implements UserDetails {

    private Long userId;

    private Long deviceId;

    private String subject;

    private String username;

    private String password;

    private Locale locale;

    private String ipAddress;

    private Collection<? extends GrantedAuthority> authorities;
}
