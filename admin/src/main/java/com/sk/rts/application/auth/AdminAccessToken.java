package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgAdminToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminAccessToken {

    private String username;

    private String token;

    private Long issueTime;

    private Long expiration;

    public AdminAccessToken(MsgAdminToken adminToken) {
        this.username = adminToken.getUsername();
        this.token = adminToken.getToken();
        this.issueTime = adminToken.getIssueTime();
        this.expiration = adminToken.getExpiration();
    }
}
