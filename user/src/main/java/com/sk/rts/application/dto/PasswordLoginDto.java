package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "密码登录")
public class PasswordLoginDto {

    @NotBlank
    @Schema(description = "用户名")
    private final  String account;

    @NotBlank
    @Schema(description = "密码")
    private final String password;

    public PasswordLoginDto(@JsonProperty("account") String account, @JsonProperty("password") String password) {
        this.account = account;
        this.password = password;
    }
}
