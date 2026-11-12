package com.sk.rts.application.dto;

import com.sk.rts.application.auth.UserAccessToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "访问TOKEN")
public class UserAccessTokenDto {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "TOKEN")
    private final String token;

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "过期时间戳")
    private final Long expireTime;

    public UserAccessTokenDto(UserAccessToken accessToken) {
        this(accessToken.getToken(), accessToken.getExpireTime());
    }

    public UserAccessTokenDto(String token, Long expireTime) {
        this.token = token;
        this.expireTime = expireTime;
    }
}
