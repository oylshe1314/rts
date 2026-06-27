package com.sk.rts.application.dto;

import com.sk.rts.application.auth.UserRefreshToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "刷新TOKEN")
public class UserRefreshTokenDto {

    @Schema(description = "TOKEN")
    private final String token;

    @Schema(description = "发布时间戳")
    private final Long issueTime;

    @Schema(description = "有效时间戳")
    private final Long expiration;

    public UserRefreshTokenDto(UserRefreshToken refreshToken) {
        this(refreshToken.getToken(), refreshToken.getIssueTime(), refreshToken.getExpireTime());
    }

    public UserRefreshTokenDto(String token, Long issueTime, Long expiration) {
        this.token = token;
        this.issueTime = issueTime;
        this.expiration = expiration;
    }
}
