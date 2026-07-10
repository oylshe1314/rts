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

    @Schema(description = "代码")
    private final String code;

    public RoleDto(Role role) {
        super(role.getId(), role.getRemark(), role.getUpdateBy(), role.getUpdateTime(), role.getState());
        this.name = role.getName();
        this.code = role.getCode();
    }
}
