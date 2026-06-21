package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgUserToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAccessToken {

    private String subject;

    private String token;

    private Long issueTime;

    private Long expiration;

    public UserAccessToken(MsgUserToken userToken) {
        this.subject = userToken.getSubject();
        this.token = userToken.getToken();
        this.issueTime = userToken.getIssueTime();
        this.expiration = userToken.getExpiration();
    }
}
