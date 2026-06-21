package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sk.rts.application.validation.NullOrNotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

@Getter
@Schema(description = "管理员查询")
public class AdminQueryDto {

    @Positive
    @Schema(description = "角色ID(下拉框选择)")
    private final Long roleId;

    @NullOrNotBlank
    @Schema(description = "用户名")
    private final String username;

    @NullOrNotBlank
    @Schema(description = "用户名")
    private final String phone;

    @NullOrNotBlank
    @Schema(description = "用户名")
    private final String email;

    public AdminQueryDto(@JsonProperty("roleId") Long roleId,
                         @JsonProperty("username") String username,
                         @JsonProperty("phone") String phone,
                         @JsonProperty("email") String email) {
        this.roleId = roleId;
        this.username = username;
        this.phone = phone;
        this.email = email;
    }
}
