package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgAdminToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AdminAccessToken {

    private String subject;

    private String token;

    public AdminAccessToken(MsgAdminToken adminToken) {
        this.subject = adminToken.getSubject();
        this.token = adminToken.getToken();
    }
}
