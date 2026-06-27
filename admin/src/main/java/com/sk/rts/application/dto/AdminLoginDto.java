package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "管理员登录信息")
public class AdminLoginDto {

    @NotBlank
    @Schema(description = "用户名")
    private final String account;

    @NotBlank
    @Schema(description = "密码")
    private final String password;

    @JsonCreator
    public AdminLoginDto(@JsonProperty("account") String account, @JsonProperty("password") String password) {
        this.account = account;
        this.password = password;
    }
}
