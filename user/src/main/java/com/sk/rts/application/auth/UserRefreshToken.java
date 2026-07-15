package com.sk.rts.application.auth;

import com.sk.rts.application.proto.caching.MsgAccessToken;
import com.sk.rts.application.proto.caching.MsgRefreshToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRefreshToken {

    private String subject;

    private Long userId;

    private Long deviceId;

    private String hash;

    private Long issueTime;

    private Long expireTime;

    private Long refreshTime;

    public UserRefreshToken(MsgRefreshToken token) {
        this.subject = token.getSubject();
        this.userId = token.getUserId();
        this.deviceId = token.getDeviceId();
        this.hash = token.getHash();
        this.issueTime = token.getIssueTime();
        this.expireTime = token.getExpireTime();
        this.refreshTime = token.getRefreshTime();
    }
}
