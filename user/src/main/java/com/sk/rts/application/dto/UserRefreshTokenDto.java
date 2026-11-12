package com.sk.rts.application.dto;

import com.sk.rts.application.auth.UserRefreshToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "刷新TOKEN")
public class UserRefreshTokenDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "主题")
    private final String subject;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "HASH")
    private final String hash;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "发布时间戳")
    private final Long issueTime;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "过期时间戳")
    private final Long expireTime;

    public UserRefreshTokenDto(UserRefreshToken refreshToken) {
        this(refreshToken.getSubject(), refreshToken.getHash(), refreshToken.getIssueTime(), refreshToken.getExpireTime());
    }

    public UserRefreshTokenDto(String subject, String hash, Long issueTime, Long expireTime) {
        this.subject = subject;
        this.hash = hash;
        this.issueTime = issueTime;
        this.expireTime = expireTime;
    }
}
