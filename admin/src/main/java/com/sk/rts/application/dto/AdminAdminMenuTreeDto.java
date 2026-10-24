package com.sk.rts.application.dto;

import com.sk.rts.application.entity.AdminMenu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "菜单树")
public class AdminAdminMenuTreeDto extends AdminMenuSortableDto<AdminAdminMenuTreeDto> {

    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "是否选中")
    private final Boolean checked;

    public AdminAdminMenuTreeDto(AdminMenu menu, Boolean checked) {
        super(menu.getId(), menu.getSortBy());
        this.parentId = menu.getParentId();
        this.name = menu.getName();
        this.checked = checked;
    }
}
