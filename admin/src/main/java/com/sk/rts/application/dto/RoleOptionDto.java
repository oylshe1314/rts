package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色选择列表")
public class RoleOptionDto extends BaseDto {

    @Schema(description = "名称")
    private final String name;

    public RoleOptionDto(Long id, String name) {
        super(id);
        this.name = name;
    }

    public RoleOptionDto(Role role) {
        super(role.getId());
        this.name = role.getName();
    }
}
