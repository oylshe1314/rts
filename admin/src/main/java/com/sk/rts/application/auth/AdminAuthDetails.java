package com.sk.rts.application.auth;

import com.sk.rts.application.entity.AdminUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

@Getter
@Setter
@NullMarked
@NoArgsConstructor
@AllArgsConstructor
public class AdminAuthDetails implements UserDetails {

    private Long adminId;
    private Long roleId;
    private String roleName;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private String ipAddress;
    private Collection<ApiPatternAuthority> authorities;

    public AdminAuthDetails(AdminUser admin, AdminRemoteDetails remoteDetails) {
        this.adminId = admin.getId();
        this.roleId = admin.getRole().getId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.password = admin.getPassword();
        this.phone = admin.getPhone();
        this.email = admin.getEmail();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
        this.ipAddress = remoteDetails.getIpAddress();
        this.authorities = Collections.emptyList();
    }

    public AdminAuthDetails(AdminUser admin, AdminRemoteDetails remoteDetails, Collection<ApiPatternAuthority> authorities) {
        this.adminId = admin.getId();
        this.roleId = admin.getRole().getId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.password = admin.getPassword();
        this.phone = admin.getPhone();
        this.email = admin.getEmail();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
        this.ipAddress = remoteDetails.getIpAddress();
        this.authorities = authorities;
    }
}

