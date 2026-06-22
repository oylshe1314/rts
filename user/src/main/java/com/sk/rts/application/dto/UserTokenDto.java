package com.sk.rts.application.dto;

import com.sk.rts.application.auth.UserTokenDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "登录TOKEN")
public class UserTokenDto {

    @Schema(description = "访问TOKEN")
    private final UserAccessTokenDto accessToken;

    @Schema(description = "刷新TOKEN, 为空则不需要刷新TOKEN")
    private final UserRefreshTokenDto refreshToken;

    public UserTokenDto(UserTokenDetails tokenDetails) {
        this(new UserAccessTokenDto(tokenDetails.getAccessToken()), tokenDetails.getRefreshToken() == null ? null : new UserRefreshTokenDto(tokenDetails.getRefreshToken()));
    }

    public UserTokenDto(UserAccessTokenDto accessToken, UserRefreshTokenDto refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
