package com.sk.rts.application.dto;

import com.sk.rts.application.dto.base.StatefulDto;
import com.sk.rts.application.entity.Menu;
import com.sk.rts.application.entity.enums.MenuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "菜单")
public class MenuDto extends StatefulDto {

    @Schema(description = "上级菜单ID")
    private final Long parentId;

    @Schema(description = "类型, 1.目录, 2.菜单, 3.接口")
    private final Integer type;

    @Schema(description = "类型名称")
    private final String typeName;

    @Schema(description = "上级菜单名称")
    private final String parentName;

    @Schema(description = "图标")
    private final String icon;

    @Schema(description = "名称")
    private final String name;

    @Schema(description = "路径")
    private final String path;

    @Schema(description = "排序值")
    private final Integer sortBy;

    public MenuDto(Menu menu) {
        super(menu.getId(), menu.getRemark(), menu.getUpdateBy(), menu.getUpdateTime(), menu.getState());
        if (menu.getParentId() == 0L) {
            this.parentId = 0L;
            this.parentName = "";
        } else {
            this.parentId = menu.getParent().getId();
            this.parentName = menu.getParent().getName();
        }
        this.type = menu.getType();
        this.typeName = MenuType.desc(this.type);
        this.icon = menu.getIcon();
        this.name = menu.getName();
        this.path = menu.getPath();
        this.sortBy = menu.getSortBy();
    }
}
