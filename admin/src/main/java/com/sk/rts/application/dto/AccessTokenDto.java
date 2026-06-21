package com.sk.rts.application.dto;

import com.sk.rts.application.auth.AdminAccessToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "访问TOKEN")
public class AccessTokenDto {

    @Schema(description = "TOKEN")
    private final String token;

    @Schema(description = "过期时间戳")
    private final Long issueTime;

    @Schema(description = "过期时间戳")
    private final Long expiration;

    public AccessTokenDto(String token, Long issueTime, Long expiration) {
        this.token = token;
        this.issueTime = issueTime;
        this.expiration = expiration;
    }

    public AccessTokenDto(AdminAccessToken accessToken) {
        this.token = accessToken.getToken();
        this.issueTime = accessToken.getIssueTime();
        this.expiration = accessToken.getExpiration();
    }
}
