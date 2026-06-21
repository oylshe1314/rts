package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "管理员添加")
public class AdminAddDto {

    @NotNull
    @Schema(description = "角色ID")
    private final Long roleId;

    @NotBlank
    @Schema(description = "用户名")
    private final String username;

    @NotBlank
    @Schema(description = "密码")
    private final String password;

    @Schema(description = "手机")
    private final String phone;

    @Schema(description = "邮箱")
    private final String email;

    @NotBlank
    @Schema(description = "昵称")
    private final String nickname;

    @Schema(description = "头像")
    private final String avatar;

    @Schema(description = "备注")
    private final String remark;

    public AdminAddDto(@JsonProperty("roleId") Long roleId,
                       @JsonProperty("username") String username,
                       @JsonProperty("password") String password,
                       @JsonProperty("email") String email,
                       @JsonProperty("account") String phone,
                       @JsonProperty("nickname") String nickname,
                       @JsonProperty("avatar") String avatar,
                       @JsonProperty("remark") String remark) {
        this.roleId = roleId;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.nickname = nickname;
        this.avatar = avatar;
        this.remark = remark;
    }
}
