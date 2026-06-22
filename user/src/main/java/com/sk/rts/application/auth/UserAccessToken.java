package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgAccessToken;
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

    public UserAccessToken(MsgAccessToken token) {
        this.subject = token.getSubject();
        this.token = token.getToken();
        this.issueTime = token.getIssueTime();
        this.expiration = token.getExpiration();
    }

    public MsgAccessToken toProto() {
        MsgAccessToken.Builder msgAccessTokenBuilder = MsgAccessToken.newBuilder();
        msgAccessTokenBuilder.setSubject(this.getSubject());
        msgAccessTokenBuilder.setToken(this.getToken());
        msgAccessTokenBuilder.setIssueTime(this.getIssueTime());
        msgAccessTokenBuilder.setExpiration(this.getExpiration());
        return msgAccessTokenBuilder.build();
    }
}
