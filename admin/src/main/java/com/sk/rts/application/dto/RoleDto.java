package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.StatefulDto;
import com.sk.rts.application.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色")
public class RoleDto extends StatefulDto {

    @Schema(description = "名称")
    private final String name;

    public RoleDto(Role role) {
        super(role.getId(), role.getRemark(), role.getUpdateBy(), role.getUpdateTime(), role.getStatus());
        this.name = role.getName();
    }
}
