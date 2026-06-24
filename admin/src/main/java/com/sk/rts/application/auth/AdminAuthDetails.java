package com.sk.rts.application.auth;

import com.sk.rts.application.entity.Admin;
import com.sk.rts.application.proto.caching.MsgAdminDetails;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

@Getter
@Setter
@NullMarked
public class AdminAuthDetails implements UserDetails {

    private Long id;
    private Long roleId;
    private String roleName;
    private String username;
    private String password;
    private String phone;
    private String email;
    private String nickname;
    private String avatar;
    private String loginIp;
    private Collection<ApiPathAuthority> authorities;

    public AdminAuthDetails(Admin admin, AdminRemoteDetails remoteDetails) {
        this.id = admin.getId();
        this.roleId = admin.getRole().getId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.password = admin.getPassword();
        this.phone = admin.getPhone();
        this.email = admin.getEmail();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
        this.loginIp = remoteDetails.getAddress();
        this.authorities = new HashSet<>();
    }

    public AdminAuthDetails(Admin admin, AdminRemoteDetails remoteDetails, Collection<ApiPathAuthority> authorities) {
        this.id = admin.getId();
        this.roleId = admin.getRole().getId();
        this.roleName = admin.getRole().getName();
        this.username = admin.getUsername();
        this.password = admin.getPassword();
        this.phone = admin.getPhone();
        this.email = admin.getEmail();
        this.nickname = admin.getNickname();
        this.avatar = admin.getAvatar();
        this.loginIp = remoteDetails.getAddress();
        this.authorities = authorities;
    }

    public AdminAuthDetails(MsgAdminDetails message) {
        this.id = message.getId();
        this.roleId = message.getRoleId();
        this.roleName = message.getRoleName();
        this.username = message.getUsername();
        this.password = message.getPassword();
        this.phone = message.getPhone();
        this.email = message.getEmail();
        this.nickname = message.getNickname();
        this.avatar = message.getAvatar();
        this.loginIp = message.getLoginIp();
        this.authorities = message.getAuthorityList().stream().map(ApiPathAuthority::new).collect(Collectors.toSet());
    }

    public static String buildTokenKey(String subject) {
        return "message:admin:token:" + subject;
    }

    public static String buildDetailsKey(long id) {
        return "message:admin:details:" + id;
    }
}

