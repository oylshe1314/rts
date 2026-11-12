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

    private AuthUser user;

    private AuthDevice device;

    private Locale locale;

    private String ipAddress;

    private Collection<? extends  GrantedAuthority> authorities;

    public Long getUserId() {
        return user.getId();
    }

    public Long getDeviceId() {
        return device.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }
}
