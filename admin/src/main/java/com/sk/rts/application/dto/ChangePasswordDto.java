package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "修改密码")
public class ChangePasswordDto {

    @NotBlank
    @Schema(description = "旧密码")
    private final String oldPassword;

    @NotBlank
    @Schema(description = "新密码")
    private final String newPassword;

    @JsonCreator
    public ChangePasswordDto(@JsonProperty("oldPassword") String oldPassword, @JsonProperty("newPassword") String newPassword) {
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
}
