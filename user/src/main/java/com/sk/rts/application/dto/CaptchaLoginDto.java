package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "密码登录")
public class CaptchaLoginDto {

    @NotBlank
    @Schema(description = "用户名")
    private final String account;

    @NotBlank
    @Schema(description = "验证码")
    private final String captcha;

    public CaptchaLoginDto(@JsonProperty("account") String account, @JsonProperty("captcha") String captcha) {
        this.account = account;
        this.captcha = captcha;
    }
}
