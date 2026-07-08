package com.sk.rts.application.dto;

import com.sk.rts.application.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Set;

@Getter
@Schema(description = "角色权限对比")
public class RoleAuthorityComparisonDto extends MenuSortableDto<RoleAuthorityComparisonDto> {

    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "类型: 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "图标")
    private final String icon;

    @Schema(description = "高亮")
    private final Boolean highlight;

    @Schema(description = "选中的角色ID")
    private final Set<Long> roles;

    public RoleAuthorityComparisonDto(Menu menu, Boolean highlight, Set<Long> roles) {
        super(menu.getId(), menu.getSortBy());
        this.parentId = menu.getParentId();
        this.type = menu.getType();
        this.name = menu.getName();
        this.icon = menu.getIcon();
        this.highlight = highlight;
        this.roles = roles;
    }

}
