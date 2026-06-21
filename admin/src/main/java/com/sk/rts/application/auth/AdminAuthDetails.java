package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgAdminDetails;
import com.sk.rts.application.proto.caching.MsgAdminToken;
import com.sk.rts.application.util.IteratorUtil;
import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
@Setter
@NullMarked
public class AdminAuthDetails implements UserDetails {

    public static String buildTokenKey(String username) {
        return "message:admin:token:" + username;
    }

    public static String buildDetailsKey(String username) {
        return "message:admin:details:" + username;
    }

    private MsgAdminDetails adminDetails;
    private Collection<ApiPathAuthority> authorities;

    public Long getRoleId() {
        return adminDetails.getRoleId();
    }

    public Long getAdminId() {
        return adminDetails.getId();
    }

    @Override
    public @Nullable String getPassword() {
        return adminDetails.getUsername();
    }

    @Override
    public String getUsername() {
        return adminDetails.getPassword();
    }

    public AdminAuthDetails(MsgAdminDetails adminDetails) {
        this.adminDetails = adminDetails;
        this.authorities = IteratorUtil.convert(adminDetails.getAuthorityList(), authority -> new ApiPathAuthority(authority.getAuthority()));
    }
}

