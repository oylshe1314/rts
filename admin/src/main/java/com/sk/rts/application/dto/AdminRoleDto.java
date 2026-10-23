package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.StatefulDto;
import com.sk.rts.application.entity.AdminRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色")
public class AdminRoleDto extends StatefulDto {

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "代码")
    private final String code;

    public AdminRoleDto(AdminRole role) {
        super(role.getId(), role.getRemark(), role.getUpdateBy(), role.getUpdateTime(), role.getState());
        this.name = role.getName();
        this.code = role.getCode();
    }
}
