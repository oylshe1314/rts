package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgUserDetails;
import com.sk.rts.application.proto.caching.MsgUserDevice;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

@Getter
@Setter
@NullMarked
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDetails implements org.springframework.security.core.userdetails.UserDetails {

    public static String buildSubject(String username, String deviceNo) {
        return username + ":" + deviceNo;
    }

    public static String buildTokenKey(String subject) {
        return "message:user:token:" + subject;
    }

    public static String buildDetailsKey(String username) {
        return "message:user:details:" + username;
    }

    public static String buildDeviceKey(String deviceNo) {
        return "message:user:device:" + deviceNo;
    }

    private MsgUserDetails details;
    private MsgUserDevice device;

    public long getUserId() {
        return details.getId();
    }

    public long getDeviceId() {
        return device.getId();
    }

    public String getDeviceNo() {
        return device.getDeviceNo();
    }

    @Override
    public String getUsername() {
        return details.getUsername();
    }

    @Override
    public String getPassword() {
        return details.getPassword();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
}
