package com.sk.rts.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "角色权限设置")
public class RoleAuthoritySetDto {

    @NotNull
    @Positive
    @Schema(description = "角色ID")
    private final Long roleId;

    @NotNull
    @Schema(description = "菜单ID列表")
    private final List<Long> menuIds;

    public RoleAuthoritySetDto(@JsonProperty("roleId") Long roleId,
                               @JsonProperty("menuIds") List<Long> menuIds) {
        this.roleId = roleId;
        this.menuIds = menuIds;
    }
}
