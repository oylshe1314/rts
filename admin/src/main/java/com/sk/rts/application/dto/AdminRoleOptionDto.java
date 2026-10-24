package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.AdminRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色选择列表")
public class AdminRoleOptionDto extends BaseDto {

    @Schema(description = "名称")
    private final String name;

    public AdminRoleOptionDto(Long id, String name) {
        super(id);
        this.name = name;
    }

    public AdminRoleOptionDto(AdminRole role) {
        super(role.getId());
        this.name = role.getName();
    }
}
