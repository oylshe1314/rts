package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.BaseDto;
import com.sk.rts.application.entity.AdminMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "菜单选择列表")
public class AdminMenuOptionDto extends BaseDto {

    @Schema(description = "名称")
    private final String name;

    public AdminMenuOptionDto(Long id, String name) {
        super(id);
        this.name = name;
    }

    public AdminMenuOptionDto(AdminMenu menu) {
        super(menu.getId());
        this.name = menu.getName();
    }
}
