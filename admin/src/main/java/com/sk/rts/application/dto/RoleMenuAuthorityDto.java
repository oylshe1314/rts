package com.sk.rts.application.dto;

import com.sk.rts.application.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "权限树")
public class RoleMenuAuthorityDto extends MenuSortableDto<RoleMenuAuthorityDto> {

    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "是否选中")
    private final Boolean checked;

    public RoleMenuAuthorityDto(Menu menu, Boolean checked) {
        super(menu.getId(), menu.getSortBy());
        this.parentId = menu.getParentId();
        this.name = menu.getName();
        this.checked = checked;
    }
}
