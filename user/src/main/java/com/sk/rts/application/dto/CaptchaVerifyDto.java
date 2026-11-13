package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.component.validation.EnumOrdinals;
import com.sk.rts.application.entity.enums.CaptchaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "验证码校验")
public class CaptchaVerifyDto {

    @NotNull
    @EnumOrdinals(value = CaptchaType.class, excludes = {"none"})
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "获取类型，1：登录验证码，2：号码绑定，3：密码设置，4：安全验证，5：号码解绑，6：账号注销")
    private final Integer type;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "账号：手机号/邮箱")
    private final String account;

    @NotBlank
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "验证码")
    private final String captcha;

    @JsonCreator
    public CaptchaVerifyDto(@JsonProperty("type") Integer type,
                            @JsonProperty("account") String account,
                            @JsonProperty("captcha") String captcha) {
        this.type = type;
        this.account = account;
        this.captcha = captcha;
    }
}
