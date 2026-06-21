package com.sk.rts.application.dto;

import com.sk.rts.application.entity.Menu;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.Set;

@Getter
@Schema(description = "角色权限对比")
public class RoleMenuAuthorityCompareDto extends MenuSortableDto<RoleMenuAuthorityCompareDto> {

    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "是否选中/高亮")
    private final Boolean highlight;

    @Schema(description = "是否选中")
    private final Set<Long> roles;

    public RoleMenuAuthorityCompareDto(Menu menu, Boolean highlight, Set<Long> roles) {
        super(menu.getId(), menu.getSortBy());
        this.parentId = menu.getParentId();
        this.name = menu.getName();
        this.highlight = highlight;
        this.roles = roles;
    }

}
