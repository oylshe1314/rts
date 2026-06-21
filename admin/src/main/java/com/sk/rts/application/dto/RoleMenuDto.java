package com.sk.rts.application.dto;

import com.sk.rts.application.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "角色菜单")
public class RoleMenuDto extends MenuSortableDto<RoleMenuDto> {

    @Schema(description = "父级ID")
    private final Long parentId;

    @Schema(description = "类型: 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "图标")
    private final String icon;

    @Schema(description = "路径")
    private final String path;

    public RoleMenuDto(Menu menu) {
        super(menu.getId(), menu.getSortBy());
        this.parentId = menu.getParentId() == null ? null : menu.getParentId();
        this.type = menu.getType();
        this.name = menu.getName();
        this.icon = menu.getIcon();
        this.path = menu.getPath();
    }
}
