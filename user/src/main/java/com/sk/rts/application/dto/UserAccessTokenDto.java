package com.sk.rts.application.dto;

import com.sk.rts.application.auth.UserAccessToken;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "访问TOKEN")
public class UserAccessTokenDto {

    @Schema(description = "TOKEN")
    private final String token;

    @Schema(description = "有效时间戳")
    private final Long expiration;

    public UserAccessTokenDto(UserAccessToken accessToken) {
        this(accessToken.getToken(), accessToken.getExpiration());
    }

    public UserAccessTokenDto(String token, Long expiration) {
        this.token = token;
        this.expiration = expiration;
    }
}
