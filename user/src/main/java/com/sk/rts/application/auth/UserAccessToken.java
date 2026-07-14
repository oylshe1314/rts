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

    private Long userId;

    private Long deviceId;

    private String token;

    private Long issueTime;

    private Long expireTime;

    public UserAccessToken(MsgAccessToken token) {
        this.subject = token.getSubject();
        this.userId = token.getUserId();
        this.deviceId = token.getDeviceId();
        this.token = token.getToken();
        this.issueTime = token.getIssueTime();
        this.expireTime = token.getExpiration();
    }
}
